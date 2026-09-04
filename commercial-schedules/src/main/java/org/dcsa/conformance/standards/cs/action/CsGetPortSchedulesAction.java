package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.PayloadPaginationCheck;
import org.dcsa.conformance.core.check.ResponseLimitCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.checks.CsChecks;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.CsRole;

import java.util.Map;
import java.util.stream.Stream;

public class CsGetPortSchedulesAction extends CsAction {

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";

  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean expectNextPageCursor;

  public CsGetPortSchedulesAction(
    String subscriberPartyName,
    String publisherPartyName,
    CsAction previousAction,
    JsonSchemaValidator responseSchemaValidator) {
    this(subscriberPartyName, publisherPartyName, previousAction, responseSchemaValidator, false);
  }

  public CsGetPortSchedulesAction(
    String subscriberPartyName,
    String publisherPartyName,
    CsAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    boolean expectNextPageCursor) {
    super(
      subscriberPartyName,
      publisherPartyName,
      previousAction,
      (previousAction instanceof CsGetPortSchedulesAction)
        ? "GetPortSchedules (second page)"
        : "GetPortSchedules",
      200);
    this.responseSchemaValidator = responseSchemaValidator;
    this.expectNextPageCursor = expectNextPageCursor;
  }

  @Override
  public String getHumanReadablePrompt() {
    return previousAction instanceof CsGetPortSchedulesAction
      ? getMarkdownHumanReadablePrompt(
      Map.of("API_PLACEHOLDER", "port schedules"),
      "prompt-consumer-get-secondpage.md",
      "prompt-consumer-refresh-complete.md")
      : getMarkdownHumanReadablePrompt(
      Map.of(
        "API_PLACEHOLDER",
        "port schedules",
        "PARAMETERS_PLACEHOLDER",
        getSuppliedScenarioParameters().toJson().toPrettyString()),
      "prompt-consumer-get.md",
      "prompt-consumer-refresh-complete.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(CsRole::isConsumer, getMatchedExchangeUuid(), "/port-schedules"),
          new ResponseStatusCheck(CsRole::isProducer, getMatchedExchangeUuid(), expectedStatus),
          new JsonSchemaCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            CsRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new HeaderCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            NEXT_PAGE_CURSOR)
            .withApplicability(expectNextPageCursor),
          new PayloadPaginationCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDynamicScenarioParameters().firstPage(),
            getDynamicScenarioParameters().secondPage())
            .withApplicability(previousAction instanceof CsGetPortSchedulesAction previous && previous.expectNextPageCursor),
          new ResponseLimitCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            () -> getSuppliedScenarioParameterValue(CsFilterParameter.LIMIT),
            "Port Schedule"),
          CsChecks.mandatoryResponseContentChecksForPs(getMatchedExchangeUuid(), expectedApiVersion),
          CsChecks.optionalResponseContentChecksForPs(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDynamicScenarioParameters();
    ObjectNode jsonActionNode = super.asJsonNode().set("suppliedScenarioParameters", getSuppliedScenarioParameters().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
  }
}
