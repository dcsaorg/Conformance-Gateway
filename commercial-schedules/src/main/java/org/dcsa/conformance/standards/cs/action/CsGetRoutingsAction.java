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

public class CsGetRoutingsAction extends CsAction {

  private static final String NEXT_PAGE_CURSOR = "Next-Page-Cursor";

  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean expectNextPageCursor;

  public CsGetRoutingsAction(
    String subscriberPartyName,
    String publisherPartyName,
    CsAction previousAction,
    JsonSchemaValidator responseSchemaValidator1) {
    this(subscriberPartyName, publisherPartyName, previousAction, responseSchemaValidator1, false);
  }

  public CsGetRoutingsAction(
    String subscriberPartyName,
    String publisherPartyName,
    CsAction previousAction,
    JsonSchemaValidator responseSchemaValidator1,
    boolean expectNextPageCursor) {
    super(
      subscriberPartyName,
      publisherPartyName,
      previousAction,
      (previousAction instanceof CsGetRoutingsAction) ? "GetRoutings (second page)" : "GetRoutings",
      200);
    this.responseSchemaValidator = responseSchemaValidator1;
    this.expectNextPageCursor = expectNextPageCursor;
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDynamicScenarioParameters();
    ObjectNode jsonActionNode =
      super.asJsonNode().set("suppliedScenarioParameters", getSuppliedScenarioParameters().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return previousAction instanceof CsGetRoutingsAction
      ? getMarkdownHumanReadablePrompt(
      Map.of("API_PLACEHOLDER", "point to point"),
      "prompt-consumer-get-secondpage.md",
      "prompt-consumer-refresh-complete.md")
      : getMarkdownHumanReadablePrompt(
      Map.of(
        "API_PLACEHOLDER",
        "point to point",
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
          new UrlPathCheck(
            CsRole::isConsumer, getMatchedExchangeUuid(), "/point-to-point-routes"),
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
            .withApplicability(previousAction instanceof CsGetRoutingsAction previous && previous.expectNextPageCursor),
          new ResponseLimitCheck(
            CsRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            () -> getSuppliedScenarioParameterValue(CsFilterParameter.LIMIT),
            "Point-to-Point Routing"),
          CsChecks.mandatoryResponseContentChecksForPtp(getMatchedExchangeUuid(), expectedApiVersion),
          CsChecks.optionalResponseContentChecksForPtp(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }
}
