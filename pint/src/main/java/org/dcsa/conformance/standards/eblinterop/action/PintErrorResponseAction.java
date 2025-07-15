package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

@Getter
@Slf4j
public class PintErrorResponseAction extends PintAction {

  public static final String INVALID_FACILITY_CODE_ATTRIBUTE = "invalidFacilityCode";

  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator requestSchemaValidator;
  private final PintResponseCode pintResponseCode;

  public PintErrorResponseAction(
      String receivingPlatform,
      String sendingPlatform,
      PintAction previousAction,
      PintResponseCode pintResponseCode,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        sendingPlatform,
        receivingPlatform,
        previousAction,
        "InvalidFacilityCode(%s)".formatted(pintResponseCode.name()),
        pintResponseCode.getHttpResponseCode());
    this.pintResponseCode = pintResponseCode;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Start an envelope transfer with an unexpected facilityCode. Not to be performed by Receivers.";
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode().put(INVALID_FACILITY_CODE_ATTRIBUTE, true);
    node.put("senderTransmissionClass", SenderTransmissionClass.VALID_ISSUANCE.name());
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
        return Stream.of(
            new HttpMethodCheck(PintRole::isSendingPlatform, getMatchedExchangeUuid(), "POST"),
            new ResponseStatusCheck(
                PintRole::isReceivingPlatform,
                getMatchedExchangeUuid(),
                pintResponseCode.getHttpResponseCode()),
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
                responseSchemaValidator));
      }
    };
  }
}
