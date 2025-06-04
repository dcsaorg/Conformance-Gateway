package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class Shipper_GetShippingInstructionsAction extends EblAction {

  private static final int FLAG_NONE = 0;
  private static final int FLAG_REQUEST_AMENDMENT = 1;
  private static final int FLAG_USE_TD_REF = 2;

  private final ShippingInstructionsStatus expectedSiStatus;
  private final ShippingInstructionsStatus expectedAmendedSiStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedStatus;
  private final boolean recordTDR;
  private final boolean useTDRef;

  private static String name(boolean requestAmendedStatus, boolean useTDRef) {
    var flag =
        (requestAmendedStatus ? FLAG_REQUEST_AMENDMENT : FLAG_NONE)
      | (useTDRef ? FLAG_USE_TD_REF : FLAG_NONE)
      ;
    return switch (flag) {
      case FLAG_NONE -> "GET SI";
      case FLAG_NONE|FLAG_USE_TD_REF -> "GET SI (TDR)";
      case FLAG_REQUEST_AMENDMENT|FLAG_NONE -> "GET aSI";
      case FLAG_REQUEST_AMENDMENT|FLAG_USE_TD_REF -> "GET aSI (TDR)";
      default -> throw new AssertionError("Missing case for 0x" + Integer.toHexString(flag));
    };
  }

  public Shipper_GetShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedAmendedSiStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus,
      boolean recordTDR,
      boolean useTDRef) {
    super(shipperPartyName, carrierPartyName, previousAction, name(requestAmendedStatus, useTDRef), 200);
    this.expectedSiStatus = expectedSiStatus;
    this.expectedAmendedSiStatus = expectedAmendedSiStatus;
    this.useTDRef = useTDRef;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedStatus = requestAmendedStatus;
    this.recordTDR = recordTDR;

    if (useTDRef && recordTDR) {
      throw new IllegalArgumentException("Cannot use recordTDR with useTDRef");
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var documentReference = this.useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
    if (documentReference == null) {
      throw new IllegalStateException("Missing document reference for use-case 3");
    }
    return super.asJsonNode()
      .put("documentReference", documentReference)
      .put("amendedContent", requestAmendedStatus);
  }

  @Override
  public String getHumanReadablePrompt() {
    var dsp = getDspSupplier().get();
    Map<String, String> replacementsMap =
        Map.ofEntries(
            Map.entry(
                "REFERENCE",
                this.useTDRef
                    ? dsp.transportDocumentReference()
                    : dsp.shippingInstructionsReference()),
            Map.entry(
                "ORIGINAL_OR_AMENDED_PLACEHOLDER", requestAmendedStatus ? "AMENDED" : "ORIGINAL"));
    return getMarkdownHumanReadablePrompt(
        replacementsMap, "prompt-shipper-get.md", "prompt-shipper-refresh-complete.md");
  }

  @Override
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
        var dsp = getDspSupplier().get();
        var documentReference =
            useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
        return Stream.of(
            new UrlPathCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                "/v3/shipping-instructions/" + documentReference),
            new ResponseStatusCheck(EblRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
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
            EBLChecks.siResponseContentChecks(
                getMatchedExchangeUuid(),
                expectedApiVersion,
                getCspSupplier(),
                getDspSupplier(),
                expectedSiStatus,
                expectedAmendedSiStatus,
                requestAmendedStatus));
      }
    };
  }
}
