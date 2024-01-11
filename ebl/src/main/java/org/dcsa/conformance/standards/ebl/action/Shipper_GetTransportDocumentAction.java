package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.*;

public class Shipper_GetTransportDocumentAction extends EblAction {

  private final TransportDocumentStatus expectedTdStatus;
  private final JsonSchemaValidator responseSchemaValidator;

  public Shipper_GetTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      TransportDocumentStatus expectedTdStatus,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET TD", 200);
    this.expectedTdStatus = expectedTdStatus;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("tdr", getDspSupplier().get().transportDocumentReference());
  }


  @Override
  public String getHumanReadablePrompt() {
    return "GET the TD with reference '%s'"
        .formatted(getDspSupplier().get().transportDocumentReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    var dsp = getDspSupplier().get();
    var tdr = dsp.transportDocumentReference() != null ? dsp.transportDocumentReference() : "<UNKNOWN-TDR>";
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                "/v3/transport-documents/" + tdr),
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
              EblRole::isCarrier,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              responseSchemaValidator),
            EBLChecks.tdContentChecks(getMatchedExchangeUuid(), expectedTdStatus, getCspSupplier(), getDspSupplier()));
      }
    };
  }
}
