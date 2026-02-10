package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
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

public class ConsumerGetEventsWithTypeAction extends TntAction {

  private final TntEventType eventType;
  private final JsonSchemaValidator responseSchemaValidator;

  public ConsumerGetEventsWithTypeAction(
      String sourcePartyName,
      String targetPartyName,
      TntAction previousAction,
      TntEventType eventType,
      JsonSchemaValidator schemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "GET Events (%s)".formatted(eventType));
    this.eventType = eventType;
    this.responseSchemaValidator = schemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "EVENT_TYPE_PARAM_NAME", TntQueryParameters.ET.getParameterName(),
            "EVENT_TYPE_VALUE", eventType.name()),
        "prompt-consumer-get-with-event-type.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put(TntConstants.EVENT_TYPE, eventType.name());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(TntRole::isConsumer, getMatchedExchangeUuid(), TntStandard.API_PATH),
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
                TntQueryParameters.ET.getParameterName(),
                eventType.name()),
            TntChecks.getTntGetResponseChecks(
                getMatchedExchangeUuid(), expectedApiVersion, eventType));
      }
    };
  }
}
