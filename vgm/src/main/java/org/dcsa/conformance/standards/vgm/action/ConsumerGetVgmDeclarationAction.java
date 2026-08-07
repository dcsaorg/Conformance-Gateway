package org.dcsa.conformance.standards.vgm.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.PayloadPaginationCheck;
import org.dcsa.conformance.core.check.QueryParamCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.vgm.checks.VgmChecks;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

public class ConsumerGetVgmDeclarationAction extends VgmAction {

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";
  private static final String NEXT_PAGE_CURSOR_STATE = "nextPageCursor";

  @Getter private final boolean hasNextPage;
  private final JsonSchemaValidator responseSchemaValidator;
  private String nextPageCursor;

  public ConsumerGetVgmDeclarationAction(
      String sourcePartyName,
      String targetPartyName,
      VgmAction previousAction,
      boolean hasNextPage,
      JsonSchemaValidator schemaValidator) {
    super(
        sourcePartyName,
        targetPartyName,
        previousAction,
        previousAction instanceof ConsumerGetVgmDeclarationAction
            ? "GET VGM Declaration (carrierBookingReference + limit + cursor)"
            : "GET VGM Declaration");
    this.hasNextPage = hasNextPage;
    this.responseSchemaValidator = schemaValidator;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    if (hasNextPage) {
      nextPageCursor = exchange.getResponse()
        .message()
        .headers()
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(NEXT_PAGE_CURSOR))
        .map(java.util.Map.Entry::getValue)
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
    } else if (previousAction instanceof ConsumerGetVgmDeclarationAction) {
      this.getDspConsumer().accept(getDspSupplier().get().withSecondPage(responseHash));
    }
  }

  private static String getHashString(String responseBody) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
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
  }

  @Override
  public ObjectNode asJsonNode() {
    SuppliedScenarioParameters suppliedScenarioParameters = safeSuppliedScenarioParameters();
    ObjectNode actionNode =
        super.asJsonNode().set("suppliedScenarioParameters", suppliedScenarioParameters.toJson());
    if (previousAction instanceof ConsumerGetVgmDeclarationAction previous && previous.nextPageCursor != null) {
      actionNode.put(VgmQueryParameters.CURSOR.getParameterName(), previous.nextPageCursor);
    }
    return actionNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    SuppliedScenarioParameters suppliedScenarioParameters = safeSuppliedScenarioParameters();
    if (suppliedScenarioParameters.getMap().isEmpty()) {
      return "Send a GET request to the sandbox endpoint '/vgm-declarations'.";
    }
    return "Send a GET request to the sandbox endpoint '/vgm-declarations' with the following query parameters: %s.%n%nThe sandbox will respond with VGM declarations matching your query parameters."
        .formatted(suppliedScenarioParameters.toJson());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.<ConformanceCheck>of(
          new UrlPathCheck(VgmRole::isConsumer, getMatchedExchangeUuid(), "/vgm-declarations"),
          new ResponseStatusCheck(VgmRole::isProducer, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            VgmRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            VgmRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            VgmRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new HeaderCheck(
            VgmRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            NEXT_PAGE_CURSOR)
            .withApplicability(hasNextPage),
          new PayloadPaginationCheck(
            VgmRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDspSupplier().get().firstPage(),
            getDspSupplier().get().secondPage())
            .withApplicability(previousAction instanceof ConsumerGetVgmDeclarationAction previous && previous.hasNextPage),
          VgmChecks.getVGMGetPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }

  private SuppliedScenarioParameters safeSuppliedScenarioParameters() {
    SuppliedScenarioParameters suppliedScenarioParameters = sspSupplier.get();
    return suppliedScenarioParameters != null
        ? suppliedScenarioParameters
        : SuppliedScenarioParameters.fromMap(Map.ofEntries());
  }
}
