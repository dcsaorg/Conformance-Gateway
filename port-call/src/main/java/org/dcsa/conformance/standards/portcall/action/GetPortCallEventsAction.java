package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.PayloadPaginationCheck;
import org.dcsa.conformance.core.check.ResponseLimitCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.checks.PortCallChecks;
import org.dcsa.conformance.standards.portcall.party.PortCallFilterParameter;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;
import org.dcsa.conformance.standards.portcall.party.ScenarioType;
import org.dcsa.conformance.standards.portcall.party.SuppliedScenarioParameters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class GetPortCallEventsAction extends PortCallAction {

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";
  private static final String NEXT_PAGE_CURSOR_STATE = "nextPageCursor";

  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean hasNextPage;
  private final ScenarioType standaloneScenarioType;
  private String nextPageCursor;

  public GetPortCallEventsAction(
    String consumerPartyName,
    String producerPartyName,
    PortCallAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    boolean hasNextPage) {
    super(
      consumerPartyName,
      producerPartyName,
      previousAction,
      previousAction instanceof GetPortCallEventsAction
        ? "GET events (portCallServiceTypeCode + limit + cursor)"
        : "GET events");
    this.responseSchemaValidator = responseSchemaValidator;
    this.hasNextPage = hasNextPage;
    this.standaloneScenarioType = null;
  }

  public GetPortCallEventsAction(
    String consumerPartyName,
    String producerPartyName,
    PortCallAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    ScenarioType scenarioType) {
    super(
      consumerPartyName,
      producerPartyName,
      previousAction,
      "GET events (%s)".formatted(scenarioType.getLabel()));
    this.responseSchemaValidator = responseSchemaValidator;
    this.hasNextPage = false;
    this.standaloneScenarioType = scenarioType;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
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
    if (previousAction instanceof SupplyScenarioParametersAction) {
      this.getDspConsumer().accept(getDspSupplier().get().withFirstPage(responseHash));
    } else if (previousAction instanceof GetPortCallEventsAction) {
      this.getDspConsumer().accept(getDspSupplier().get().withSecondPage(responseHash));
    }
  }

  private static String getHashString(String responseBody) {
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
    if (standaloneScenarioType != null) {
      this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(standaloneScenarioType.name()));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode actionNode = super.asJsonNode().set("suppliedScenarioParameters", safeSuppliedScenarioParameters().toJson());
    if (previousAction instanceof GetPortCallEventsAction previous && previous.nextPageCursor != null) {
      actionNode.put(PortCallFilterParameter.CURSOR.getQueryParamName(), previous.nextPageCursor);
    }
    return actionNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET request to the sandbox endpoint '/events'.\n\nThe sandbox will respond with at least one Port Call event that demonstrates %s."
      .formatted(standaloneScenarioType.getLabel());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.<ConformanceCheck>of(
          new UrlPathCheck(PortCallRole::isConsumer, getMatchedExchangeUuid(), "/events"),
          new ResponseStatusCheck(PortCallRole::isProducer, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            PortCallRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new HeaderCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            NEXT_PAGE_CURSOR)
            .withApplicability(hasNextPage),
          new PayloadPaginationCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDspSupplier().get().firstPage(),
            getDspSupplier().get().secondPage())
            .withApplicability(previousAction instanceof GetPortCallEventsAction previous && previous.hasNextPage),
          new ResponseLimitCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            () -> sspSupplier.get().getMap().get(PortCallFilterParameter.LIMIT),
            "Event",
            "events"),
          PortCallChecks.getGetResponsePayloadChecks(getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
      }
    };
  }

  private SuppliedScenarioParameters safeSuppliedScenarioParameters() {
    SuppliedScenarioParameters suppliedScenarioParameters = sspSupplier.get();
    return suppliedScenarioParameters != null
      ? suppliedScenarioParameters
      : SuppliedScenarioParameters.fromMap(Map.of());
  }
}

