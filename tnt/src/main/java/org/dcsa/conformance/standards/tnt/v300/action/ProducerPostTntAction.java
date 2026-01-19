package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.v300.checks.TntChecks;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

public class ProducerPostTntAction extends TntAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final TntEventType eventType;

  public ProducerPostTntAction(
      String sourcePartyName,
      String targetPartyName,
      TntAction previousAction,
      TntEventType eventType,
      JsonSchemaValidator requestSchemaValidator) {
    super(
        sourcePartyName, targetPartyName, previousAction, "Producer POST Action for " + eventType);
    this.eventType = eventType;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your system POST a message containing at least one event of type %s"
        .formatted(eventType);
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("eventType", eventType.name());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(TntRole::isProducer, getMatchedExchangeUuid(), "/events"),
            new ResponseStatusCheck(TntRole::isConsumer, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                TntRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new ApiHeaderCheck(
                TntRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new JsonSchemaCheck(
                TntRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator)
            //,TntChecks.getTntPostPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion, eventType.name())
        );
      }
    };
  }
}
