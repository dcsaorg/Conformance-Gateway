package org.dcsa.conformance.standards.eblinterop.action;

import static org.dcsa.conformance.standards.eblinterop.checks.PintChecks.validateSignedFinishResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintCloseTransferAction extends PintAction {
  private final PintResponseCode pintResponseCode;
  private final JsonSchemaValidator responseSchemaValidator;

  public PintCloseTransferAction(
    String receivingPlatform,
    String sendingPlatform,
    PintAction previousAction,
    PintResponseCode pintResponseCode,
    JsonSchemaValidator responseSchemaValidator
    ) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "FinishTransfer(%s)".formatted(pintResponseCode.name()),
        pintResponseCode.getHttpResponseCode()
    );
    this.pintResponseCode = pintResponseCode;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Finish the transfer-transaction");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
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
        var envelopeReference = Objects.requireNonNullElse(getDsp().envelopeReference(), "<MISSING-REFERENCE>");

        return Stream.of(
                new UrlPathCheck(
                    PintRole::isSendingPlatform, getMatchedExchangeUuid(), "/envelopes/%s/finish-transfer".formatted(envelopeReference)),
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
                validateSignedFinishResponse(
                  getMatchedExchangeUuid(),
                  expectedApiVersion,
                  pintResponseCode
                )
            );
      }
    };
  }
}
