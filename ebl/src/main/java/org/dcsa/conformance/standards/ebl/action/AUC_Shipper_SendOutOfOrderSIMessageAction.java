package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.models.OutOfOrderMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
@Slf4j
public class AUC_Shipper_SendOutOfOrderSIMessageAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final OutOfOrderMessageType outOfOrderMessageType;
  private final boolean useTDRef;

  public AUC_Shipper_SendOutOfOrderSIMessageAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      OutOfOrderMessageType outOfOrderMessageType,
      boolean useTDRef,
      JsonSchemaValidator requestSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "AUC-OOOM [%s]".formatted(outOfOrderMessageType.getUC()), 409);
    if (!useTDRef && outOfOrderMessageType.isTDRequest()) {
      throw new IllegalArgumentException("useTDRef must be true for " + outOfOrderMessageType.name());
    }
    this.outOfOrderMessageType = outOfOrderMessageType;
    this.useTDRef = useTDRef;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    var dsp = getDspSupplier().get();
    return ("AUC: Send an out of order but otherwise valid message of type %s (%s) to the document reference %s".formatted(
      outOfOrderMessageType.name(),
      outOfOrderMessageType.getUC(),
      useTDRef ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference()
    ));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var sir = dsp.shippingInstructionsReference();
    return super.asJsonNode()
      .put("outOfOrderMessageType", this.outOfOrderMessageType.name())
      .put("documentReference", useTDRef ? dsp.transportDocumentReference() : sir)
      .put("sir", sir);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var urlFormat = outOfOrderMessageType.getExpectedRequestUrlFormat();
        String documentReference;
        if (useTDRef) {
          documentReference = Objects.requireNonNullElse(dsp.transportDocumentReference(), "<DSP MISSING TD REFERENCE>");
        } else {
          documentReference = Objects.requireNonNullElse(dsp.shippingInstructionsReference(), "<DSP MISSING SI REFERENCE>");
        }
        return Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), outOfOrderMessageType.getExpectedRequestMethod()),
            new UrlPathCheck(EblRole::isShipper, getMatchedExchangeUuid(), urlFormat.formatted(documentReference)),
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
                requestSchemaValidator));
      }
    };
  }
}
