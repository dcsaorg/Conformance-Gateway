package org.dcsa.conformance.standards.ebl.action;



import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class Shipper_GetShippingInstructionsAction extends EblAction {

  private static final int FLAG_NONE = 0;
  private static final int FLAG_REQUEST_AMENDMENT = 1;

  private final ShippingInstructionsStatus expectedSiStatus;
  private final ShippingInstructionsStatus expectedAmendedSiStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedStatus;
  private final boolean recordTDR;
  private final boolean useBothRef;

  private static String name(boolean requestAmendedStatus) {
    var flag = (requestAmendedStatus ? FLAG_REQUEST_AMENDMENT : FLAG_NONE);
    return switch (flag) {
      case FLAG_NONE -> "GET SI";
      case FLAG_REQUEST_AMENDMENT -> "GET aSI";
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
      boolean useBothRef) {
    super(
        shipperPartyName, carrierPartyName, previousAction, name(requestAmendedStatus), 200, true);
    this.expectedSiStatus = expectedSiStatus;
    this.expectedAmendedSiStatus = expectedAmendedSiStatus;
    this.useBothRef = useBothRef;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedStatus = requestAmendedStatus;
    this.recordTDR = recordTDR;

    if (useBothRef && recordTDR) {
      throw new IllegalArgumentException("Cannot use recordTDR with useBothRef");
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var documentReference =
        this.useBothRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
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
                this.useBothRef
                    ? String.format(
                        "either the TD reference (%s) or the SI reference (%s)",
                        dsp.transportDocumentReference(), dsp.shippingInstructionsReference())
                    : "document reference  " + dsp.shippingInstructionsReference()),
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
      var tdr =
          exchange.getResponse().message().body().getJsonBody().path("transportDocumentReference");
      if (!tdr.isMissingNode()) {
        getDspConsumer().accept(dsp.withTransportDocumentReference(tdr.asText()));
      }
    }
  }

  @Override
  public Set<String> skippableForRoles() {
    return Set.of(EblRole.SHIPPER.getConfigName());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();

        return Stream.of(
            new UrlPathCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                buildFullUris(
                    "/v3/shipping-instructions/",
                    dsp.shippingInstructionsReference(),
                    dsp.transportDocumentReference())),
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
            EblChecks.siResponseContentChecks(
                getMatchedExchangeUuid(),
                expectedApiVersion,
                expectedSiStatus,
                expectedAmendedSiStatus,
                requestAmendedStatus,
                getDspSupplier()));
      }
    };
  }
}
