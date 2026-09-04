package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.PayloadPaginationCheck;
import org.dcsa.conformance.core.check.QueryParamCheck;
import org.dcsa.conformance.core.check.ResponseLimitCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.dcsa.conformance.standards.an.checks.ANQueryParameterChecks;
import org.dcsa.conformance.standards.an.party.ANRole;

import java.util.Map;
import java.util.stream.Stream;

public class SubscriberGetANAction extends ANAction {

  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean expectNextPageCursor;

  public SubscriberGetANAction(
    String subscriberPartyName,
    String publisherPartyName,
    ANAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    String title,
    boolean expectNextPageCursor) {
    super(subscriberPartyName, publisherPartyName, previousAction, title);
    this.responseSchemaValidator = responseSchemaValidator;
    this.expectNextPageCursor = expectNextPageCursor;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your application GET from its counterpart running in the sandbox the Arrival Notices with the following transport document references: "
      + getDspSupplier().get().suppliedQueryParameters();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        Stream.Builder<ConformanceCheck> checks = Stream.builder();
        checks.add(new UrlPathCheck(ANRole::isConsumer, getMatchedExchangeUuid(), "/arrival-notices"));
        checks.add(new ResponseStatusCheck(ANRole::isProducer, getMatchedExchangeUuid(), 200));
        checks.add(
          new JsonSchemaCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator));
        checks.add(
          new ApiHeaderCheck(
            ANRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion));
        checks.add(
          new ApiHeaderCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion));
        Map<String, String> supplied = getDspSupplier().get().suppliedQueryParameters();
        if (supplied != null) {
          supplied.forEach(
            (name, value) ->
              checks.add(
                new QueryParamCheck(
                  ANRole::isConsumer, getMatchedExchangeUuid(), name, value)));
        }
        String cursor = getDspSupplier().get().cursor();
        if (previousAction instanceof SubscriberGetANAction && cursor != null) {
          checks.add(
            new QueryParamCheck(
              ANRole::isConsumer, getMatchedExchangeUuid(), "cursor", cursor));
        }
        checks.add(
          new HeaderCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            "Next-Page-Cursor")
            .withApplicability(expectNextPageCursor));
        checks.add(
          new PayloadPaginationCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDspSupplier().get().firstPageHash(),
            getDspSupplier().get().secondPageHash())
            .withApplicability(previousAction instanceof SubscriberGetANAction));
        if (supplied != null && supplied.containsKey("limit")) {
          checks.add(
            new ResponseLimitCheck(
              ANRole::isProducer,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              () -> supplied.get("limit"),
              "Arrival Notice",
              "arrivalNotices"));
        }
        if (getDspSupplier().get().scenarioType() != null) {
          checks.add(
            ANChecks.getANGetResponseChecks(
              getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
          checks.add(
            ANQueryParameterChecks.matchingResponse(
              getMatchedExchangeUuid(),
              expectedApiVersion,
              () -> getDspSupplier().get().suppliedQueryParameters()));
        }
        return checks.build();
      }
    };
  }


  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode =
      super.asJsonNode()
        .set(
          "suppliedQueryParameters",
          org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER.valueToTree(
            getDspSupplier().get().suppliedQueryParameters() == null
              ? Map.of()
              : getDspSupplier().get().suppliedQueryParameters()));
    if (getDspSupplier().get().cursor() != null) {
      jsonNode.put("cursor", getDspSupplier().get().cursor());
    }
    return jsonNode;
  }
}
