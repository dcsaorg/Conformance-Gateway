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
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;

public class ShipperGetTransportDocumentErrorAction extends EblAction {

  private static final int RESPONSE_CODE = 404;
  public static final String SEND_INVALID_DOCUMENT_REFERENCE = "sendInvalidDocumentReference";

  private final JsonSchemaValidator responseSchemaValidator;

  public ShipperGetTransportDocumentErrorAction(
      String shipperPartyName,
      String carrierPartyName,
      EblAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "GET (Non existing transport document)",
        RESPONSE_CODE);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("documentReference", getDspSupplier().get().shippingInstructionsReference())
        .put(SEND_INVALID_DOCUMENT_REFERENCE, true);
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of(), "prompt-shipper-get-transport-document-error-scenario.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "GET"),
            new ResponseStatusCheck(EblRole::isCarrier, getMatchedExchangeUuid(), RESPONSE_CODE),
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
                responseSchemaValidator));
      }
    };
  }
}
