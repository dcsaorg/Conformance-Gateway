package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SI_NORMALIZER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
            responseContentChecks(expectedApiVersion),
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

  private ActionCheck responseContentChecks(String expectedApiVersion) {

    return new ActionCheck(
        "Check if the SI has changed",
        EblRole::isCarrier,
        getMatchedExchangeUuid(),
        HttpMessageType.RESPONSE) {

      @Override
      public Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
        JsonNode nodeToCheck;
        if (previousAction.getMatchedExchangeUuid() == null) {
          return Set.of();
        }

        final UUID compareToExchangeUuid = previousAction.getMatchedExchangeUuid();
        ConformanceExchange previousExchange = getExchangeByUuid.apply(compareToExchangeUuid);

        if (requestAmendedStatus) {
          nodeToCheck = previousExchange.getResponse().message().body().getJsonBody();
        } else if (previousAction
            instanceof UC4_Carrier_ProcessUpdateToShippingInstructionsAction) { // checkUC4 accept
          var tempAction = previousAction;
          while (!(tempAction instanceof UC3ShipperSubmitUpdatedShippingInstructionsAction)) {
            tempAction = tempAction.getPreviousAction();
          }
          previousExchange = getExchangeByUuid.apply(tempAction.getMatchedExchangeUuid());
          nodeToCheck = previousExchange.getRequest().message().body().getJsonBody();
        } else {
          var action = previousAction;
          while (!(action instanceof UC1_Shipper_SubmitShippingInstructionsAction)) {
            action = action.getPreviousAction();
          }
          previousExchange = getExchangeByUuid.apply(action.getMatchedExchangeUuid());
          nodeToCheck = previousExchange.getRequest().message().body().getJsonBody();
        }

        JsonNode finalNodeToCheck = nodeToCheck;
        return JsonAttribute.contentChecks(
                "",
                "Validate that shipper provided data was not altered",
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion,
                JsonAttribute.lostAttributeCheck(
                    "Validate that shipper provided data was not altered",
                    () -> finalNodeToCheck,
                    SI_NORMALIZER))
            .performCheckConformance(getExchangeByUuid);
      }
    };
  }
}
