package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
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
import org.dcsa.conformance.standards.tnt.TntStandard;
import org.dcsa.conformance.standards.tnt.v300.checks.TntChecks;
import org.dcsa.conformance.standards.tnt.v300.party.TntConstants;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

import java.util.Map;
import java.util.stream.Stream;

public class ConsumerGetEventsWithQueryParametersAction extends TntAction {

  @Getter
  private final boolean hasNextPage;
  private final JsonSchemaValidator responseSchemaValidator;

  public ConsumerGetEventsWithQueryParametersAction(
    String sourcePartyName,
    String targetPartyName,
    TntAction previousAction,
    boolean hasNextPage,
    JsonSchemaValidator schemaValidator) {
    super(
      sourcePartyName,
      targetPartyName,
      previousAction,
      previousAction instanceof ConsumerGetEventsWithQueryParametersAction
        ? "GET Events (next page)"
        : "GET Events");
    this.responseSchemaValidator = schemaValidator;
    this.hasNextPage = hasNextPage;
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode = super.asJsonNode().set(TntConstants.SUPPLIED_SCENARIO_PARAMETERS, sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put(TntQueryParameters.CURSOR.getParameterName(), cursor);
    }
    return jsonActionNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(Map.of(), "prompt-consumer-get.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(
            TntRole::isConsumer, getMatchedExchangeUuid(), TntStandard.API_PATH),
          new ResponseStatusCheck(TntRole::isProducer, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            TntRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            TntRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            TntRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new HeaderCheck(
            TntRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            TntConstants.HEADER_CURSOR_NAME)
            .withApplicability(hasNextPage),
          new PayloadPaginationCheck(
            TntRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            getDspSupplier().get().firstPage(),
            getDspSupplier().get().secondPage())
            .withApplicability(previousAction instanceof ConsumerGetEventsWithQueryParametersAction previous && previous.hasNextPage),
          new ResponseLimitCheck(
            TntRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            () -> sspSupplier.get().getMap().get(TntQueryParameters.LIMIT),
            "Event",
            TntConstants.EVENTS),
          TntChecks.getTntGetResponseChecks(getMatchedExchangeUuid(), expectedApiVersion, null));
      }
    };
  }
}
