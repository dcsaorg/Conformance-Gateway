package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HeaderCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.QueryParamCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.TntStandard;
import org.dcsa.conformance.standards.tnt.v300.checks.TntChecks;
import org.dcsa.conformance.standards.tnt.v300.party.TntConstants;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

public class ConsumerGetEventsWithQueryParametersAction extends TntAction {

  @Getter private final boolean hasNextPage;
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
    ObjectNode jsonActionNode =
        super.asJsonNode()
            .set(TntConstants.SUPPLIED_SCENARIO_PARAMETERS, sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put(TntQueryParameters.CURSOR.getParameterName(), cursor);
    }
    return jsonActionNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (previousAction instanceof ConsumerGetEventsWithQueryParametersAction) {
      return """
          Send a GET request to the sandbox endpoint '/events' to fetch the next results page, using the cursor retrieved from the headers of the response of the previous GET request.

          Query parameters:
          - cursor=%s
          """
          .formatted(getDspSupplier().get().cursor());
    }

    var params = sspSupplier.get();
    if (params == null || params.getMap().isEmpty()) {
      return """
          Send a GET request to the sandbox endpoint '/events'.

          The sandbox will respond with events matching your query parameters.""";
    }

    StringBuilder prompt =
        new StringBuilder(
            """
        Send a GET request to the sandbox endpoint '/events' with the following query parameters:

        """);

    params
        .getMap()
        .forEach(
            (key, value) ->
                prompt
                    .append("- ")
                    .append(key.getParameterName())
                    .append("=")
                    .append(value)
                    .append("\n"));

    prompt.append("\nThe sandbox will respond with events matching your query parameters.");

    return prompt.toString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var checks =
            Stream.of(
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
                new QueryParamCheck(
                        TntRole::isConsumer,
                        getMatchedExchangeUuid(),
                        TntQueryParameters.CURSOR.getParameterName(),
                        getDspSupplier().get().cursor())
                    .withApplicability(
                        previousAction
                                instanceof ConsumerGetEventsWithQueryParametersAction previous
                            && previous.hasNextPage),
                new HeaderCheck(
                        TntRole::isProducer,
                        getMatchedExchangeUuid(),
                        HttpMessageType.RESPONSE,
                        TntConstants.HEADER_CURSOR_NAME)
                    .withApplicability(hasNextPage),
                TntChecks.getTntGetResponseChecks(getMatchedExchangeUuid(), expectedApiVersion, null));

        var queryParamChecks =
            sspSupplier.get() == null
                ? Stream.<QueryParamCheck>empty()
                : sspSupplier.get().getMap().entrySet().stream()
                    .map(
                        entry ->
                            new QueryParamCheck(
                                TntRole::isConsumer,
                                getMatchedExchangeUuid(),
                                entry.getKey().getParameterName(),
                                entry.getValue()));

        return Stream.concat(checks, queryParamChecks);
      }
    };
  }
}
