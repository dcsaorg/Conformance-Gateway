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

  public static final String SEND_INVALID_FACILITY_CODE = "sendInvalidFacilityCode";
  private static final int RESPONSE_CODE = 400;

  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator requestSchemaValidator;

  public PintErrorResponseAction(
      String receivingPlatform,
      String sendingPlatform,
      PintAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator) {
    super(sendingPlatform, receivingPlatform, previousAction, "IncorrectSingleRequestTransfer", RESPONSE_CODE);
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Not relevant for human operators: used only by synthetic sending parties when starting an envelope transfer with an invalid facilityCode.";
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode().put(SEND_INVALID_FACILITY_CODE, true);
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
                PintRole::isReceivingPlatform, getMatchedExchangeUuid(), RESPONSE_CODE),
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
