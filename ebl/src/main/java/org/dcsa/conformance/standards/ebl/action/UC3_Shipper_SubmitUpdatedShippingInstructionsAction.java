package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class UC3_Shipper_SubmitUpdatedShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean useTDRef;

  public UC3_Shipper_SubmitUpdatedShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      boolean useTDRef,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC3" + (useTDRef ? " [TDR]" : ""), Set.of(200, 202));
    this.useTDRef = useTDRef;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  protected DynamicScenarioParameters updateDSPFromSIHook(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    var body = exchange.getRequest().message().body().getJsonBody();
    return dynamicScenarioParameters.withUpdatedShippingInstructions(body);
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC3: Submit an updated shipping instructions request using the following parameters:");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var documentReference = this.useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
    if (documentReference == null) {
      throw new IllegalStateException("Missing document reference for use-case 3");
    }
    return super.asJsonNode()
      .put("sir", dsp.shippingInstructionsReference())
      .put("documentReference", documentReference);
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var currentState = Objects.requireNonNullElse(
          dsp.shippingInstructionsStatus(),
          ShippingInstructionsStatus.SI_RECEIVED  // Placeholder to avoid NPE
        );
        Stream<ActionCheck> primaryExchangeChecks =
          Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PUT"),
            new UrlPathCheck(EblRole::isShipper, getMatchedExchangeUuid(), "/v3/shipping-instructions/%s".formatted(useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference())),
            new ResponseStatusCheck(
                EblRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new JsonSchemaCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            EBLChecks.siRequestContentChecks(getMatchedExchangeUuid(), expectedApiVersion, getCspSupplier(), getDspSupplier())
        );
        return Stream.concat(
          primaryExchangeChecks,
          Stream.concat(
              EBLChecks.siRefStatusContentChecks(
                getMatchedExchangeUuid(),
                expectedApiVersion,
                currentState,
                ShippingInstructionsStatus.SI_UPDATE_RECEIVED,
                EBLChecks.sirInRefStatusMustMatchDSP(getDspSupplier())),
              getSINotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                currentState,
                ShippingInstructionsStatus.SI_UPDATE_RECEIVED,
                EBLChecks.sirInNotificationMustMatchDSP(getDspSupplier())))
          );
      }
    };
  }
}
