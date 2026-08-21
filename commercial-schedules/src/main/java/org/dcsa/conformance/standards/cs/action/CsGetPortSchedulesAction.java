package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.checks.CsChecks;
import org.dcsa.conformance.standards.cs.party.CsRole;

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
                sspSupplier.get().toJson().toPrettyString()),
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
                    getDspSupplier().get().firstPage(),
                    getDspSupplier().get().secondPage())
                .withApplicability(
                    previousAction instanceof CsGetPortSchedulesAction previous
                        && previous.expectNextPageCursor),
            CsChecks.mandatoryResponseContentChecksForPs(
                getMatchedExchangeUuid(), expectedApiVersion),
            CsChecks.optionalResponseContentChecksForPs(
                getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode = super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
  }
}
