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
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintInitiateTransferUnsignedErrorAction extends PintAction {
  private final SenderTransmissionClass senderTransmissionClass;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator envelopeEnvelopeSchemaValidator;
  private final JsonSchemaValidator envelopeTransferChainEntrySchemaValidator;
  private final JsonSchemaValidator issuanceManifestSchemaValidator;

  public PintInitiateTransferUnsignedErrorAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    int expectedStatus,
    SenderTransmissionClass senderTransmissionClass,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
    JsonSchemaValidator issuanceManifestSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "StartTransfer",
        expectedStatus
    );
    this.senderTransmissionClass = senderTransmissionClass;
    this.requestSchemaValidator = requestSchemaValidator;
    this.envelopeEnvelopeSchemaValidator = envelopeEnvelopeSchemaValidator;
    this.envelopeTransferChainEntrySchemaValidator = envelopeTransferChainEntrySchemaValidator;
    this.issuanceManifestSchemaValidator = issuanceManifestSchemaValidator;
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
        Supplier<SignatureVerifier> carrierVerifierSupplier = () -> resolveSignatureVerifierCarrierSignatures();

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
    };
  }
}
