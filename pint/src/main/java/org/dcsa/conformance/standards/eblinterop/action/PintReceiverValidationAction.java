package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintReceiverValidationAction extends PintAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;

  public PintReceiverValidationAction(
    String sourcePartyName,
    String targetPartyName,
    PintAction previousAction,
    int responseCode,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator
    ) {
    super(
        sourcePartyName,
        targetPartyName,
        previousAction,
        "ReceiverValidation(%d)".formatted(responseCode),
        responseCode
    );
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("RECEIVER_VALIDATION", getDsp().receiverValidation().toString()),
        "prompt-receiver-validation.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
    node.set("dsp", getDsp().toJson());
    return node;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {

        return Stream.of(
                new HttpMethodCheck(PintRole::isSendingPlatform, getMatchedExchangeUuid(), "POST"),
                new UrlPathCheck(
                    PintRole::isSendingPlatform, getMatchedExchangeUuid(), "/receiver-validation"),
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
                  PintRole::isSendingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  requestSchemaValidator
                ),
                new JsonSchemaCheck(
                  PintRole::isReceivingPlatform,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  responseSchemaValidator
                )
            );
      }
    };
  }
}
