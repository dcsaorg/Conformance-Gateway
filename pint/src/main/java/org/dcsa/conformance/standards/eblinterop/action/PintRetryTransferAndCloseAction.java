package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.ebl.crypto.SignedNodeSupport.parseSignedNodeNoErrors;
import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintRetryTransferAndCloseAction extends PintAction {
  private final PintResponseCode responseCode;
  private final SenderTransmissionClass senderTransmissionClass;
  private final RetryType retryType;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;
  private final JsonSchemaValidator issuanceManifestSchemaValidator;

  public PintRetryTransferAndCloseAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    PintResponseCode responseCode,
    SenderTransmissionClass senderTransmissionClass,
    RetryType retryType,
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
        "RetryTransfer(%s, %s)".formatted(responseCode.name(), retryType.name()),
        responseCode.getHttpResponseCode()
    );
    this.responseCode = responseCode;
    this.senderTransmissionClass = senderTransmissionClass;
    this.retryType = retryType;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.envelopeEnvelopeSchemaValidator = envelopeEnvelopeSchemaValidator;
    this.issuanceManifestSchemaValidator = issuanceManifestSchemaValidator;
    this.envelopeTransferChainEntrySchemaValidator = envelopeTransferChainEntrySchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return switch (retryType) {
      case NO_CHANGE -> getMarkdownHumanReadablePrompt("prompt-retry-transfer-no-change.md");
      case RESIGN -> getMarkdownHumanReadablePrompt("prompt-retry-transfer-resign.md");
      case MANIPULATE -> getMarkdownHumanReadablePrompt("prompt-retry-transfer-manipulate.md");
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode()
      .put("senderTransmissionClass", senderTransmissionClass.name())
      .put("retryType", retryType.name());
    node.set("rsp", getRsp().toJson());
    node.set("ssp", getSsp().toJson());
    node.set("dsp", getDsp().toJson());
    return node;
  }

  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDsp();
    boolean dspChanged = false;
    var requestBody = exchange.getRequest().message().body().getJsonBody();
    if (dsp.documentChecksums().isEmpty()) {
      var envelopeNode = parseSignedNodeNoErrors(
        requestBody.path("envelopeManifestSignedContent")
      );
      var supportingDocuments = envelopeNode.path("supportingDocuments");
      var visualizationChecksum = envelopeNode.path("eBLVisualisationByCarrier").path("documentChecksum").asText(null);

      var missingDocuments = StreamSupport.stream(supportingDocuments.spliterator(), false)
        .map(n -> n.path("documentChecksum"))
        .filter(JsonNode::isTextual)
        .map(JsonNode::asText)
        .collect(Collectors.toSet());

      if (visualizationChecksum != null) {
        missingDocuments.add(visualizationChecksum);
      }
      dsp = dsp.withDocumentChecksums(Set.copyOf(missingDocuments));
      dspChanged = true;
    }
    var response = exchange.getResponse().message().body().getJsonBody();
    var envelopeReference = response.path("envelopeReference").asText();
    if (envelopeReference != null) {
      dsp = dsp.withEnvelopeReference(envelopeReference);
      dspChanged = true;
    }

    if (dspChanged) {
        setDsp(dsp);
    }
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Supplier<SignatureVerifier> senderVerifierSupplier = () -> resolveSignatureVerifierSenderSignatures();
        Supplier<SignatureVerifier> carrierVerifierSupplier = () -> resolveSignatureVerifierCarrierSignatures();
        Supplier<SignatureVerifier> receiverVerifierSupplier = () -> resolveSignatureVerifierForReceiverSignatures();

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
                validateRequestSignatures(
                  getMatchedExchangeUuid(),
                  expectedApiVersion,
                  senderVerifierSupplier,
                  carrierVerifierSupplier
                ),
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
                validateInitiateTransferRequest(
                  getMatchedExchangeUuid(),
                  expectedApiVersion,
                  senderTransmissionClass,
                  () -> getSsp(),
                  () -> getRsp(),
                  () -> getDsp()
                ),
                validateSignedFinishResponse(
                  getMatchedExchangeUuid(),
                  expectedApiVersion,
                  responseCode
                )
            );
      }
    };
  }
}
