package org.dcsa.conformance.standards.vgm.action;

import static org.dcsa.conformance.core.report.ConformanceStatus.PARTIALLY_CONFORMANT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.QueryParamCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.vgm.checks.VgmChecks;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

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
      return "Send a GET request to the sandbox endpoint '/vgm-declarations'. This are the possible query parameters you can use: %s.%n%nThe sandbox will respond with VGM declarations matching your query parameters."
          .formatted(Arrays.stream(VgmQueryParameters.values()).map(VgmQueryParameters::getParameterName).toList());
    }
    return "Send a GET request to the sandbox endpoint '/vgm-declarations' with the following query parameters: %s.%n%nThe sandbox will respond with VGM declarations matching your query parameters."
        .formatted(suppliedScenarioParameters.toJson());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        SuppliedScenarioParameters suppliedScenarioParameters = safeSuppliedScenarioParameters();
        Stream<ConformanceCheck> defaultChecks = Stream.of(
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
          new QueryParamCheck(
            VgmRole::isConsumer,
            getMatchedExchangeUuid(),
            VgmQueryParameters.CURSOR.getParameterName(),
            previousAction instanceof ConsumerGetVgmDeclarationAction previous
              ? previous.nextPageCursor
              : null)
            .withApplicability(previousAction instanceof ConsumerGetVgmDeclarationAction previous
              && previous.hasNextPage),
          VgmChecks.getVGMGetPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion));

        Stream<QueryParamCheck> suppliedParameterChecks =
          suppliedScenarioParameters.getMap().entrySet().stream()
            .map(
              entry ->
                new QueryParamCheck(
                  VgmRole::isConsumer,
                  getMatchedExchangeUuid(),
                  entry.getKey().getParameterName(),
                  entry.getValue()));

        return Stream.concat(defaultChecks, suppliedParameterChecks);
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
