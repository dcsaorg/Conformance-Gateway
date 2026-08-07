package org.dcsa.conformance.standards.ovs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.PayloadPaginationCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.checks.OvsChecks;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

@Getter
@Slf4j
public class OvsGetSchedulesAction extends OvsAction {

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";
  private static final String NEXT_PAGE_CURSOR_STATE = "nextPageCursor";

  private final boolean hasNextPage;
  private final JsonSchemaValidator responseSchemaValidator;
  private String nextPageCursor;

  public OvsGetSchedulesAction(
    String subscriberPartyName,
    String publisherPartyName,
    ConformanceAction previousAction,
    boolean hasNextPage,
    JsonSchemaValidator responseSchemaValidator) {
    super(
      subscriberPartyName,
      publisherPartyName,
      previousAction,
      previousAction instanceof OvsGetSchedulesAction
        ? "GET service schedules (cursor)"
        : "GET service schedules");
    this.hasNextPage = hasNextPage;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    if (hasNextPage) {
      nextPageCursor =
        exchange.getResponse().message().headers().entrySet().stream()
          .filter(entry -> entry.getKey().equalsIgnoreCase(NEXT_PAGE_CURSOR))
          .map(Map.Entry::getValue)
          .flatMap(Collection::stream)
          .findFirst()
          .orElse(null);
    }
    updatePageHash(exchange);
  }

  private void updatePageHash(ConformanceExchange exchange) {
    String responseHash = getHashString(exchange.getResponse().message().body().toString());
    if (responseHash == null) {
      return;
    }
    if (previousAction instanceof SupplyScenarioParametersAction) {
      this.getDspConsumer().accept(getDspSupplier().get().withFirstPage(responseHash));
    } else if (previousAction instanceof OvsGetSchedulesAction) {
      this.getDspConsumer().accept(getDspSupplier().get().withSecondPage(responseHash));
    }
  }

  private String getHashString(String responseBody) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Hashing of the response failed.", e);
      return null;
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET request to the endpoint '/service-schedules'.";
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode actionNode = super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    if (previousAction instanceof OvsGetSchedulesAction previous && previous.nextPageCursor != null) {
      actionNode.put("cursor", previous.nextPageCursor);
    }
    return actionNode;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode state = super.exportJsonState();
    if (nextPageCursor != null) {
      state.put(NEXT_PAGE_CURSOR_STATE, nextPageCursor);
    }
    return state;
  }

  @Override
  public void importJsonState(JsonNode state) {
    super.importJsonState(state);
    nextPageCursor = state.path(NEXT_PAGE_CURSOR_STATE).textValue();
  }

  @Override
  public void reset() {
    super.reset();
    nextPageCursor = null;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(OvsRole::isConsumer, getMatchedExchangeUuid(), "/service-schedules"),
          new ResponseStatusCheck(OvsRole::isProducer, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            OvsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            OvsRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            OvsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new HeaderCheck(
            OvsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            NEXT_PAGE_CURSOR)
            .withApplicability(hasNextPage),
          new PayloadPaginationCheck(
            OvsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDspSupplier().get().firstPage(),
            getDspSupplier().get().secondPage())
            .withApplicability(previousAction instanceof OvsGetSchedulesAction previous && previous.hasNextPage),
          OvsChecks.mandatoryResponseContentChecks(getMatchedExchangeUuid(), expectedApiVersion),
          OvsChecks.optionalResponseContentChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }
}
