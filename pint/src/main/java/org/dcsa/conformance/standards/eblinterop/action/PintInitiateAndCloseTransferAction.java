package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateInitiateTransferRequest;
import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateSignedFinishResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.checks.PintChecks;
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintInitiateAndCloseTransferAction extends PintAction {
  private final PintResponseCode pintResponseCode;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;

  public PintInitiateAndCloseTransferAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    PintResponseCode pintResponseCode,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
    JsonSchemaValidator responseSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "SingleRequestTransfer(%s)".formatted(pintResponseCode.name()),
        200
    );
    this.pintResponseCode = pintResponseCode;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.envelopeEnvelopeSchemaValidator = envelopeEnvelopeSchemaValidator;
    this.envelopeTransferChainEntrySchemaValidator = envelopeTransferChainEntrySchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Send transfer-transaction request");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode()
      .put("transportDocumentReference", getSsp().transportDocumentReference());
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

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Supplier<SignatureVerifier> senderVerifierSupplier = () -> PayloadSignerFactory.testKeySignatureVerifier();
        return Stream.of(
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
                JsonAttribute.contentChecks(
                  PintRole::isSendingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  JsonAttribute.customValidator("envelopeManifestSignedContent signature could be validated", JsonAttribute.path("envelopeManifestSignedContent", PintChecks.signatureValidates(senderVerifierSupplier))),
                  JsonAttribute.allIndividualMatchesMustBeValid("envelopeTransferChain signature could be validated", mav -> mav.submitAllMatching("envelopeTransferChain.*"), PintChecks.signatureValidates(senderVerifierSupplier))
                ),
                JsonAttribute.contentChecks(
                  PintRole::isSendingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  JsonAttribute.customValidator("envelopeManifestSignedContent matches schema", JsonAttribute.path("envelopeManifestSignedContent", PintChecks.signedContentSchemaValidation(envelopeEnvelopeSchemaValidator))),
                  JsonAttribute.allIndividualMatchesMustBeValid("envelopeTransferChain matches schema", mav -> mav.submitAllMatching("envelopeTransferChain.*"), PintChecks.signedContentSchemaValidation(envelopeTransferChainEntrySchemaValidator))
                ),
                new JsonSchemaCheck(
                        PintRole::isSendingPlatform,
                        getMatchedExchangeUuid(),
                        HttpMessageType.REQUEST,
                        requestSchemaValidator
                ),
                validateInitiateTransferRequest(
                  getMatchedExchangeUuid(),
                  () -> getSsp(),
                  () -> getRsp(),
                  () -> getDsp()
                ),
                validateSignedFinishResponse(
                  getMatchedExchangeUuid(),
                  pintResponseCode
                )
            );
      }
    };
  }
}
