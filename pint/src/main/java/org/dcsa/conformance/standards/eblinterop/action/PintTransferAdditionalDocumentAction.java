package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.checks.AdditionalDocumentUrlPathAndContentCheck;
import org.dcsa.conformance.standards.eblinterop.checks.PintChecks;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintTransferAdditionalDocumentAction extends PintAction {
  private final JsonSchemaValidator errorResponseSchemaValidator;
  private final SenderDocumentTransmissionTypeCode senderDocumentTransmissionTypeCode;

  public PintTransferAdditionalDocumentAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    SenderDocumentTransmissionTypeCode senderDocumentTransmissionTypeCode,
    JsonSchemaValidator errorResponseSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "TransferAdditionalDocument(%s)".formatted(senderDocumentTransmissionTypeCode.name()),
        senderDocumentTransmissionTypeCode.getHttpResponseCode()
    );
    this.senderDocumentTransmissionTypeCode = senderDocumentTransmissionTypeCode;
    this.errorResponseSchemaValidator = errorResponseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Send transfer-transaction request");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
    node.put("senderDocumentTransmissionTypeCode", senderDocumentTransmissionTypeCode.name());
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
                senderDocumentTransmissionTypeCode != SenderDocumentTransmissionTypeCode.CORRUPTED_DOCUMENT
                  ? new AdditionalDocumentUrlPathAndContentCheck(
                      PintRole::isSendingPlatform,
                      getMatchedExchangeUuid(),
                      delayedValue(dspSupplier, DynamicScenarioParameters::envelopeReference))
                  : null,
                new ResponseStatusCheck(
                    PintRole::isReceivingPlatform, getMatchedExchangeUuid(), expectedStatus),
                new ApiHeaderCheck(
                    PintRole::isSendingPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    expectedApiVersion),
                new ApiHeaderCheck(
                    PintRole::isReceivingPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    expectedApiVersion),
                senderDocumentTransmissionTypeCode.getHttpResponseCode() != 204
                    ? new JsonSchemaCheck(
                        PintRole::isReceivingPlatform,
                        getMatchedExchangeUuid(),
                        HttpMessageType.RESPONSE,
                        errorResponseSchemaValidator
                      )
                    : null,
                senderDocumentTransmissionTypeCode.getHttpResponseCode() != 204
                  ? PintChecks.validateSignedFinishResponse(
                      getMatchedExchangeUuid(),
                      expectedApiVersion,
                      PintResponseCode.INCD
                    )
                  : null
                /*
                new JsonSchemaCheck(
                    PintRole::isSendingPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    requestSchemaValidator
                )
                 */
                )
            .filter(Objects::nonNull);
      }
    };
  }
}
