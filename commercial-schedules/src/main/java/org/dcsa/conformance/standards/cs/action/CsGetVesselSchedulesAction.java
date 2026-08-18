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

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";

  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean expectNextPageCursor;

  public CsGetVesselSchedulesAction(
      String subscriberPartyName,
      String publisherPartyName,
      CsAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    this(subscriberPartyName, publisherPartyName, previousAction, responseSchemaValidator, false);
  }

  public CsGetVesselSchedulesAction(
      String subscriberPartyName,
      String publisherPartyName,
      CsAction previousAction,
      JsonSchemaValidator responseSchemaValidator,
      boolean expectNextPageCursor) {
    super(
        subscriberPartyName,
        publisherPartyName,
        previousAction,
        (previousAction instanceof CsGetVesselSchedulesAction)
            ? "GetVesselSchedules (second page)"
            : "GetVesselSchedules",
        200);
    this.responseSchemaValidator = responseSchemaValidator;
    this.expectNextPageCursor = expectNextPageCursor;
  }


  @Override
  public String getHumanReadablePrompt() {
    return previousAction instanceof CsGetVesselSchedulesAction
        ? getMarkdownHumanReadablePrompt(
            Map.of("API_PLACEHOLDER", "vessel schedules"),
            "prompt-consumer-get-secondpage.md",
            "prompt-consumer-refresh-complete.md")
        : getMarkdownHumanReadablePrompt(
            Map.of(
                "API_PLACEHOLDER",
                "vessel schedules",
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
            new UrlPathCheck(CsRole::isConsumer, getMatchedExchangeUuid(), "/vessel-schedules"),
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
                    previousAction instanceof CsGetVesselSchedulesAction previous
                        && previous.expectNextPageCursor),
            CsChecks.getPayloadChecksForVs(getMatchedExchangeUuid(), expectedApiVersion));
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
