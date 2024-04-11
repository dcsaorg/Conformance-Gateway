package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.checks.AdditionalDocumentUrlPathAndContentCheck;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintTransferAdditionalDocumentFailureAction extends PintAction {

  public PintTransferAdditionalDocumentFailureAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    int expectedStatus
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "TransferAdditionalDocument(%d)".formatted(expectedStatus),
        expectedStatus
    );
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Send transfer-transaction request");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
    node.put("senderDocumentTransmissionTypeCode", SenderDocumentTransmissionTypeCode.VALID_DOCUMENT.name());
    node.set("rsp", getRsp().toJson());
    node.set("ssp", getSsp().toJson());
    node.set("dsp", getDsp().toJson());
    return node;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Supplier<DynamicScenarioParameters> dspSupplier = () -> getDsp();
        return Stream.of(
                new AdditionalDocumentUrlPathAndContentCheck(
                      PintRole::isSendingPlatform,
                      getMatchedExchangeUuid(),
                      delayedValue(dspSupplier, DynamicScenarioParameters::envelopeReference)),
                new ResponseStatusCheck(
                    PintRole::isReceivingPlatform, getMatchedExchangeUuid(), expectedStatus),
                new ApiHeaderCheck(
                    PintRole::isSendingPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    expectedApiVersion)
                );
      }
    };
  }
}
