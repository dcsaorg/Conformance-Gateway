package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.checks.CsChecks;
import org.dcsa.conformance.standards.cs.party.CsRole;

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
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode =
        super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
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
            "prompt-subscriber-get-secondpage.md",
            "prompt-subscriber-refresh-complete.md")
        : getMarkdownHumanReadablePrompt(
            Map.of(
                "API_PLACEHOLDER",
                "point to point",
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
                    getDspSupplier().get().firstPage(),
                    getDspSupplier().get().secondPage())
                .withApplicability(
                    previousAction instanceof CsGetRoutingsAction previous
                        && previous.expectNextPageCursor),
            CsChecks.getPayloadChecksForPtp(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }
}
