package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.ANRole;

public class PublisherPostANAction extends AnAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private ScenarioType scenarioType;

  public PublisherPostANAction(
      String publisherPartyName,
      String subscriberPartyName,
      AnAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator requestSchemaValidator) {

    super(publisherPartyName, subscriberPartyName, previousAction, "POST AN");
    this.requestSchemaValidator = requestSchemaValidator;
    this.scenarioType = scenarioType;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Submit a Arrival Notice to the subscriber.\n";
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("isFreighted", scenarioType.equals(ScenarioType.FREIGHTED));
    jsonNode.put("isFreetime", scenarioType.equals(ScenarioType.FREE_TIME));
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new JsonSchemaCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator));
      }
    };
  }
}
