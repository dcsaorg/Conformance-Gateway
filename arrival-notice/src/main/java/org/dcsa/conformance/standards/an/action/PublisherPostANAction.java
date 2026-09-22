package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.stream.Stream;

public class PublisherPostANAction extends ANAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final ScenarioType scenarioType;
  private final boolean validateProducerPayload;

  public PublisherPostANAction(
    String publisherPartyName,
    String subscriberPartyName,
    ANAction previousAction,
    ScenarioType scenarioType,
    JsonSchemaValidator requestSchemaValidator,
    String title) {
    this(
      publisherPartyName,
      subscriberPartyName,
      previousAction,
      scenarioType,
      requestSchemaValidator,
      title,
      true);
  }

  public PublisherPostANAction(
    String publisherPartyName,
    String subscriberPartyName,
    ANAction previousAction,
    ScenarioType scenarioType,
    JsonSchemaValidator requestSchemaValidator,
    String title,
    boolean validateProducerPayload) {

    super(publisherPartyName, subscriberPartyName, previousAction, title);
    this.requestSchemaValidator = requestSchemaValidator;
    this.scenarioType = scenarioType;
    this.validateProducerPayload = validateProducerPayload;
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
        Stream<ConformanceCheck> defaultChecks = Stream.of(
          new UrlPathCheck(ANRole::isProducer, getMatchedExchangeUuid(), "/arrival-notices"),
          new ResponseStatusCheck(ANRole::isConsumer, getMatchedExchangeUuid(), 200),
          new ApiHeaderCheck(
            ANRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ApiHeaderCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new JsonSchemaCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            requestSchemaValidator));
        return validateProducerPayload
          ? Stream.concat(defaultChecks, Stream.of(ANChecks.getANPostPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion, scenarioType.name())))
          : defaultChecks;
      }
    };
  }

}
