package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.checks.CsChecks;
import org.dcsa.conformance.standards.cs.party.CsRole;

@Getter
@Slf4j
public class CsGetVesselSchedulesAction extends CsAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public CsGetVesselSchedulesAction(
      String subscriberPartyName,
      String publisherPartyName,
      CsAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        subscriberPartyName,
        publisherPartyName,
        previousAction,
        (previousAction instanceof CsGetVesselSchedulesAction)
            ? "GetVesselSchedules (second page)"
            : "GetVesselSchedules",
        200);
    this.responseSchemaValidator = responseSchemaValidator;
  }


  @Override
  public String getHumanReadablePrompt() {
    return previousAction instanceof CsGetVesselSchedulesAction
        ? getMarkdownHumanReadablePrompt(
            Map.of("API_PLACEHOLDER", "vessel schedules"),
            "prompt-subscriber-get-secondpage.md",
            "prompt-subscriber-refresh-complete.md")
        : getMarkdownHumanReadablePrompt(
            Map.of(
                "API_PLACEHOLDER",
                "vessel schedules",
                "PARAMETERS_PLACEHOLDER",
                sspSupplier.get().toJson().toPrettyString()),
            "prompt-subscriber-get.md",
            "prompt-subscriber-refresh-complete.md");
  }


  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(CsRole::isSubscriber, getMatchedExchangeUuid(), "/vessel-schedules"),
            new ResponseStatusCheck(CsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new JsonSchemaCheck(
                CsRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                CsRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                CsRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            CsChecks.getPayloadChecksForVs(
                getMatchedExchangeUuid(),
                expectedApiVersion,
                getDspSupplier(),
                previousAction instanceof CsGetVesselSchedulesAction));
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode =
        super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
  }
}
