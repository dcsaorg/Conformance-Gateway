package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class Shipper_GetShippingInstructionsAction extends EblAction {

  private final ShippingInstructionsStatus expectedSiStatus;
  private final ShippingInstructionsStatus expectedAmendedSiStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedStatus;

  public Shipper_GetShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedAmendedSiStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET", 200);
    this.expectedSiStatus = expectedSiStatus;
    this.expectedAmendedSiStatus = expectedAmendedSiStatus;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedStatus = requestAmendedStatus;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("sir", getDspSupplier().get().shippingInstructionsReference());
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the SI with reference '%s'"
        .formatted(getDspSupplier().get().shippingInstructionsReference());
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
                "/v2/shipping-instructions/" + getDspSupplier().get().shippingInstructionsReference()),
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
