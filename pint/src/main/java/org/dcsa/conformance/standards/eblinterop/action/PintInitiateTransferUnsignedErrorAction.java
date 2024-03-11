package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.tdContentChecks;
import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateInitiateTransferRequest;
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
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintInitiateTransferUnsignedErrorAction extends PintAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;

  public PintInitiateTransferUnsignedErrorAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    int expectedStatus,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "StartTransfer",
        expectedStatus
    );
    this.requestSchemaValidator = requestSchemaValidator;
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
      .put("senderTransmissionClass", SenderTransmissionClass.VALID_ISSUANCE.name());
    node.set("rsp", getRsp().toJson());
    node.set("ssp", getSsp().toJson());
    node.set("dsp", getDsp().toJson());
    return node;
  }

  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDsp();
    var td = exchange.getRequest().message().body().getJsonBody().path("transportDocument");
    boolean dspChanged = false;
    if (!td.isMissingNode() && dsp.transportDocumentChecksum() == null) {
      var checksum = Checksums.sha256CanonicalJson(td);
      dsp = dsp.withTransportDocumentChecksum(checksum);
      dspChanged = true;
    }
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
                JsonAttribute.contentChecks(
                  PintRole::isSendingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  JsonAttribute.customValidator("envelopeManifestSignedContent signature could be validated", JsonAttribute.path("envelopeManifestSignedContent", PintChecks.signatureValidates(senderVerifierSupplier))),
                  JsonAttribute.allIndividualMatchesMustBeValid("envelopeManifestSignedContent signature could be validated", mav -> mav.submitAllMatching("envelopeTransferChain.*"), PintChecks.signatureValidates(senderVerifierSupplier))
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
                tdContentChecks(
                  getMatchedExchangeUuid(),
                  () -> getSsp()
                ),
                validateInitiateTransferRequest(
                  getMatchedExchangeUuid(),
                  () -> getSsp(),
                  () -> getRsp(),
                  () -> getDsp()
                )
            );
      }
    };
  }
}
