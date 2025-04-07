package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintInitiateAndCloseTransferAction extends PintAction {
  private final PintResponseCode pintResponseCode;
  private final SenderTransmissionClass senderTransmissionClass;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;
  private final JsonSchemaValidator issuanceManifestSchemaValidator;

  public PintInitiateAndCloseTransferAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    PintResponseCode pintResponseCode,
    SenderTransmissionClass senderTransmissionClass,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
    JsonSchemaValidator issuanceManifestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "SingleRequestTransfer(%s)".formatted(pintResponseCode.name()),
        pintResponseCode.getHttpResponseCode()
    );
    this.pintResponseCode = pintResponseCode;
    this.senderTransmissionClass = senderTransmissionClass;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.envelopeEnvelopeSchemaValidator = envelopeEnvelopeSchemaValidator;
    this.envelopeTransferChainEntrySchemaValidator = envelopeTransferChainEntrySchemaValidator;
    this.issuanceManifestSchemaValidator = issuanceManifestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (isInvalidRequest()) {
      return ("Send an invalid transfer-transaction request: Not to be performed by implementers");
    }
    return ("Send transfer-transaction request");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode()
      .put("senderTransmissionClass", senderTransmissionClass.name());
    node.set("rsp", getRsp().toJson());
    node.set("ssp", getSsp().toJson());
    node.set("dsp", getDsp().toJson());
    return node;
  }

  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDsp();
    var td = exchange.getRequest().message().body().getJsonBody().path("transportDocument");
    if (!td.isMissingNode() && dsp.transportDocumentChecksum() == null) {
      var checksum = Checksums.sha256CanonicalJson(td);
      setDsp(dsp.withTransportDocumentChecksum(checksum));
    }
  }

  private boolean shouldValidateSignature() {
    return senderTransmissionClass != SenderTransmissionClass.SIGNATURE_ISSUE
      && senderTransmissionClass != SenderTransmissionClass.INVALID_PAYLOAD;
  }

  private boolean shouldValidateRequestPayload() {
    return isInvalidRequest();
  }

  private boolean isInvalidRequest() {
    return senderTransmissionClass != SenderTransmissionClass.INVALID_PAYLOAD;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Supplier<SignatureVerifier> senderVerifierSupplier = () -> resolveSignatureVerifierSenderSignatures();
        Supplier<SignatureVerifier> carrierVerifierSupplier = () -> resolveSignatureVerifierCarrierSignatures();
        Supplier<SignatureVerifier> receiverVerifierSupplier = () -> resolveSignatureVerifierForReceiverSignatures();

        var requestChecks = List.<ConformanceCheck>of();

        if (shouldValidateRequestPayload()) {
          requestChecks = List.of(
            validateInnerRequestSchemas(
              getMatchedExchangeUuid(),
              expectedApiVersion,
              envelopeEnvelopeSchemaValidator,
              envelopeTransferChainEntrySchemaValidator,
              issuanceManifestSchemaValidator
            ),
            JsonAttribute.contentChecks(
              "",
              "The signatures of the signed content of the HTTP response can be validated",
              PintRole::isReceivingPlatform,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              expectedApiVersion,
              JsonAttribute.customValidator(
                "Response signature must be valid",
                SignatureChecks.signatureValidates(receiverVerifierSupplier)
              )
            ),
            new JsonSchemaCheck(
              PintRole::isSendingPlatform,
              getMatchedExchangeUuid(),
              HttpMessageType.REQUEST,
              requestSchemaValidator
            ),
            tdContentChecks(
              getMatchedExchangeUuid(),
              expectedApiVersion,
              () -> getSsp()
            ),
            validateInitiateTransferRequest(
              getMatchedExchangeUuid(),
              expectedApiVersion,
              senderTransmissionClass,
              () -> getSsp(),
              () -> getRsp(),
              () -> getDsp()
            )
          );
        }
        var genericChecks = Stream.of(
          new UrlPathCheck(
            PintRole::isSendingPlatform, getMatchedExchangeUuid(), "/envelopes"),
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
          new JsonSchemaCheck(
            PintRole::isReceivingPlatform,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator
          ),
          shouldValidateSignature()
            ? validateRequestSignatures(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            senderVerifierSupplier,
            carrierVerifierSupplier
          )
            : null,
          validateSignedFinishResponse(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            pintResponseCode
          )
        );

        return Stream.concat(genericChecks, requestChecks.stream()).filter(Objects::nonNull);
      }
    };
  }
}
