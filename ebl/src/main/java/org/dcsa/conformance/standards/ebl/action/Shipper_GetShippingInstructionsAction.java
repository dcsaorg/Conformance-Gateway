package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class Shipper_GetShippingInstructionsAction extends EblAction {

  private final ShippingInstructionsStatus expectedSiStatus;
  private final ShippingInstructionsStatus expectedAmendedSiStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedStatus;
  private final boolean recordTDR;

  public Shipper_GetShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedAmendedSiStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus,
      boolean recordTDR) {
    super(shipperPartyName, carrierPartyName, previousAction, requestAmendedStatus ? "GET aSI" : "GET SI", 200);
    this.expectedSiStatus = expectedSiStatus;
    this.expectedAmendedSiStatus = expectedAmendedSiStatus;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedStatus = requestAmendedStatus;
    this.recordTDR = recordTDR;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("sir", getDspSupplier().get().shippingInstructionsReference());
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the SI with reference '%s'"
        .formatted(getDspSupplier().get().shippingInstructionsReference());
  }

  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    if (recordTDR) {
      var dsp = getDspSupplier().get();
      var tdr = exchange.getResponse().message().body().getJsonBody().path("transportDocumentReference");
      if (!tdr.isMissingNode()) {
        getDspConsumer().accept(dsp.withTransportDocumentReference(tdr.asText()));
      }
    }
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                "/v3/shipping-instructions/" + getDspSupplier().get().shippingInstructionsReference()),
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
                expectedApiVersion));
      }
    };
  }
}
