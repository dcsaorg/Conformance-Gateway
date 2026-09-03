package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;
import org.dcsa.conformance.standards.ebl.models.CarrierShippingInstructions;
import org.dcsa.conformance.standards.ebl.party.EblRole;

public class UC17_Shipper_SubmitTransportDocumentAmendmentAction extends ShipperNotificationEblAction {
  private static final ScenarioType STANDALONE_SCENARIO_TYPE = ScenarioType.REGULAR_STRAIGHT_BL;

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final ObjectNode standaloneAmendedTransportDocument;

  public UC17_Shipper_SubmitTransportDocumentAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications,
      String standardVersion) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC17", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.standaloneAmendedTransportDocument =
        previousAction == null ? createStandaloneAmendment(standardVersion) : null;
    initializeStandaloneScenarioParameters();
  }

  @Override
  public void reset() {
    super.reset();
    initializeStandaloneScenarioParameters();
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", String.valueOf(getDSP().transportDocumentReference())),
        "prompt-shipper-uc17.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    initializeStandaloneScenarioParameters();
    ObjectNode prompt =
        super.asJsonNode()
        .put("tdr", getDspSupplier().get().transportDocumentReference())
        .put("scenarioType", getDspSupplier().get().scenarioType());
    if (standaloneAmendedTransportDocument != null) {
      prompt.set("amendedTransportDocument", standaloneAmendedTransportDocument.deepCopy());
    } else {
      var suppliedPayload = getCarrierPayloadSupplier().get();
      if (suppliedPayload != null && suppliedPayload.has("amendedTransportDocument")) {
        prompt.set("amendedTransportDocument", suppliedPayload.required("amendedTransportDocument"));
      }
    }
    return prompt;
  }

  private static ObjectNode createStandaloneAmendment(String standardVersion) {
    ObjectNode shippingInstructions =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/ebl/messages/" + STANDALONE_SCENARIO_TYPE.eblPayload(standardVersion),
                Map.of());
    ObjectNode amendment =
        CarrierShippingInstructions.createTransportDocumentFromShippingInstructions(
            shippingInstructions, standardVersion, STANDALONE_SCENARIO_TYPE);
    amendment.put(
        "serviceContractReference",
        amendment.path("serviceContractReference").asText("Ref-123") + "-AMENDED");
    return amendment;
  }

  private void initializeStandaloneScenarioParameters() {
    if (standaloneAmendedTransportDocument != null) {
      getDspConsumer()
          .accept(
              getDspSupplier()
                  .get()
                  .withScenarioType(STANDALONE_SCENARIO_TYPE.name())
                  .withTransportDocumentReference(
                      standaloneAmendedTransportDocument
                          .required("transportDocumentReference")
                          .asText()));
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    String tdr =
        exchange
            .getRequest()
            .message()
            .body()
            .getJsonBody()
            .required("transportDocumentReference")
            .asText();
    getDspConsumer().accept(getDspSupplier().get().withTransportDocumentReference(tdr));
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
        String tdr = getDspSupplier().get().transportDocumentReference();
        Stream<ConformanceCheck> requestChecks =
            Stream.of(
                new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PUT"),
                new UrlPathCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    "/v3/transport-documents/%s/amendment".formatted(tdr)),
                ResponseStatusCheck.forSuccessfulResponse(
                    EblRole::isCarrier, getMatchedExchangeUuid()),
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
                EblChecks.shipperAmendmentContentChecks(
                    getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
        return Stream.concat(
            requestChecks,
            getTDNotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                TransportDocumentStatusScenario.uc17()));
      }
    };
  }
}


