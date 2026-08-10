package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.checks.PortCallChecks;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;
import org.dcsa.conformance.standards.portcall.party.ScenarioType;

import java.util.stream.Stream;

public class PostPortCallEventsAction extends PortCallAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final ScenarioType scenarioType;

  public PostPortCallEventsAction(
    String producerPartyName,
    String consumerPartyName,
    PortCallAction previousAction,
    ScenarioType scenarioType,
    JsonSchemaValidator requestSchemaValidator) {
    super(
      producerPartyName,
      consumerPartyName,
      previousAction,
      "POST event (%s)".formatted(scenarioType.getLabel()));
    this.scenarioType = scenarioType;
    this.requestSchemaValidator = requestSchemaValidator;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a POST request to the sandbox endpoint '/events' with at least one Port Call event that demonstrates %s."
        .formatted(scenarioType.getLabel());
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("scenarioType", scenarioType.name());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.<ConformanceCheck>of(
          new UrlPathCheck(PortCallRole::isProducer, getMatchedExchangeUuid(), "/events"),
          new ResponseStatusCheck(PortCallRole::isConsumer, getMatchedExchangeUuid(), 200),
          new ApiHeaderCheck(
            PortCallRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ApiHeaderCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new JsonSchemaCheck(
            PortCallRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            requestSchemaValidator),
          PortCallChecks.getPortCallPostPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
      }
    };
  }
}

