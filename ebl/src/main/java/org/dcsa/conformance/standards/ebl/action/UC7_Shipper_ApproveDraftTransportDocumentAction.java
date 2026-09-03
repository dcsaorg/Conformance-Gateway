package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;
import org.dcsa.conformance.standards.ebl.party.EblRole;

@Getter
@Slf4j
public class UC7_Shipper_ApproveDraftTransportDocumentAction extends ShipperNotificationEblAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC7_Shipper_ApproveDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "UC7",
        202,
        isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getTransportDocumentReference()),
        "prompt-shipper-uc7.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("documentReference", getTransportDocumentReference());
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    // Clear the flag if set.
    if (dsp.newTransportDocumentContent()) {
      getDspConsumer().accept(dsp.withNewTransportDocumentContent(false));
    }
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
        Stream<ActionCheck> primaryExchangeChecks =
            Stream.of(
                new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
                new UrlPathCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    getTransportDocumentEndpoint()),
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
                EblChecks.shipperApprovalContentChecks(
                    getMatchedExchangeUuid(), expectedApiVersion),
                EblChecks.tdRefStatusChecks(
                    getMatchedExchangeUuid(),
                    expectedApiVersion,
                    getDspSupplier(),
                    TransportDocumentStatusScenario.uc7()));
        return Stream.concat(
            primaryExchangeChecks,
            getTDNotificationChecks(
                    getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                TransportDocumentStatusScenario.uc7()));
      }
    };
  }
}
