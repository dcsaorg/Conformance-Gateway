package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateInitiateTransferRequest;
import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateUnsignedStartResponse;
import static org.dcsa.conformance.standards.eblinterop.crypto.SignedNodeSupport.parseSignedNodeNoErrors;

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
import org.dcsa.conformance.standards.eblinterop.checks.PintChecks;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintRetryTransferAction extends PintAction {
  private final int expectedMissingDocCount;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;

  public PintRetryTransferAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    int expectedMissingDocCount,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
    JsonSchemaValidator responseSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "RetryTransfer(MD:%d)".formatted(expectedMissingDocCount),
        201
    );
    this.expectedMissingDocCount = expectedMissingDocCount;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.envelopeEnvelopeSchemaValidator = envelopeEnvelopeSchemaValidator;
    this.envelopeTransferChainEntrySchemaValidator = envelopeTransferChainEntrySchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Retry transfer-transaction request");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode()
      .put("senderTransmissionClass", SenderTransmissionClass.VALID_ISSUANCE.name());
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
                  expectedApiVersion,
                  JsonAttribute.customValidator("envelopeManifestSignedContent signature could be validated", JsonAttribute.path("envelopeManifestSignedContent", PintChecks.signatureValidates(senderVerifierSupplier))),
                  JsonAttribute.allIndividualMatchesMustBeValid("envelopeManifestSignedContent signature could be validated", mav -> mav.submitAllMatching("envelopeTransferChain.*"), PintChecks.signatureValidates(senderVerifierSupplier))
                ),
                JsonAttribute.contentChecks(
                  PintRole::isSendingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  expectedApiVersion,
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
                  expectedApiVersion,
                  () -> getSsp(),
                  () -> getRsp(),
                  () -> getDsp()
                ),
                validateUnsignedStartResponse(
                  getMatchedExchangeUuid(),
                  expectedApiVersion,
                  expectedMissingDocCount,
                  () -> getDsp()
                )
            );
      }
    };
  }
}
