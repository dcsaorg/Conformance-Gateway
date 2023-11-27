package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class EblAction extends ConformanceAction {
  protected final int expectedStatus;

  public EblAction(
      String sourcePartyName,
      String targetPartyName,
      EblAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected EblAction getPreviousEblAction() {
    return (EblAction) previousAction;
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousEblAction().getCspConsumer();
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return getPreviousEblAction().getDspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousEblAction().getCspSupplier();
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return getPreviousEblAction().getDspSupplier();
  }

  protected void storeCbrAndCbrrIfPresent(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    if (getDspSupplier().get().shippingInstructionsReference() == null) {
      if (responseJsonNode.has("shippingInstructionsReference")) {
        getDspConsumer()
            .accept(
                new DynamicScenarioParameters(
                    responseJsonNode.get("shippingInstructionsReference").asText(),
                    getDspSupplier().get().transportDocumentReference()));
      }
    }
    if (getDspSupplier().get().transportDocumentReference() == null) {
      if (responseJsonNode.has("transportDocumentReference")) {
        getDspConsumer()
            .accept(
                new DynamicScenarioParameters(
                    getDspSupplier().get().shippingInstructionsReference(),
                    responseJsonNode.get("transportDocumentReference").asText()));
      }
    }
  }

  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion,
      JsonSchemaValidator notificationSchemaValidator) {
    String titlePrefix = "[Notification]";
    var cbr = getDspSupplier().get().transportDocumentReference();
    var cbrr = getDspSupplier().get().shippingInstructionsReference();
    return Stream.of(
            new HttpMethodCheck(
                titlePrefix, EblRole::isCarrier, getMatchedNotificationExchangeUuid(), "POST"),
            new UrlPathCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                "/v2/ebl-notifications"),
            new ResponseStatusCheck(
                titlePrefix, EblRole::isShipper, getMatchedNotificationExchangeUuid(), 204),
            new ApiHeaderCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                titlePrefix,
                EblRole::isShipper,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                titlePrefix,
                EblRole::isCarrier,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                notificationSchemaValidator))
        .filter(Objects::nonNull);
  }
}
