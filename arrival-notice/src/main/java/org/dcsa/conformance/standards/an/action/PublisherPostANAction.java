package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.ANRole;

public class PublisherPostANAction extends ANAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final ScenarioType scenarioType;

  public PublisherPostANAction(
      String publisherPartyName,
      String subscriberPartyName,
      ANAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator requestSchemaValidator) {

    super(publisherPartyName, subscriberPartyName, previousAction, computeTitle(scenarioType));
    this.requestSchemaValidator = requestSchemaValidator;
    this.scenarioType = scenarioType;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your application POST one or more Arrival Notices to its synthetic counterpart running in the sandbox";
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("scenarioType", scenarioType.name());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(ANRole::isPublisher, getMatchedExchangeUuid(), "/arrival-notices"),
            new ResponseStatusCheck(ANRole::isSubscriber, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new ApiHeaderCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new JsonSchemaCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            ANChecks.getANPostPayloadChecks(
                getMatchedExchangeUuid(), expectedApiVersion, scenarioType.name()));
      }
    };
  }


  private static String computeTitle(ScenarioType scenarioType) {
    if (scenarioType == ScenarioType.FREIGHTED) {
      return "POST AN [%s]".formatted("FREIGHTED");
    } else if (scenarioType == ScenarioType.FREE_TIME) {
      return "POST AN [%s]".formatted("FREE_TIME");
    } else {
      return "POST AN";
    }
  }
}
