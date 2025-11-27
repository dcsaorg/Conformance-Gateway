package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
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

public class PublisherPostPortCallEventsAction extends PortCallAction{

  private final JsonSchemaValidator requestSchemaValidator;
  private final ScenarioType scenarioType;

  public PublisherPostPortCallEventsAction(
      String publisherPartyName,
      String subscriberPartyName,
      PortCallAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator requestSchemaValidator) {
    super(
        publisherPartyName,
        subscriberPartyName,
        previousAction,
        "POST Port Call Events(%s)".formatted(scenarioType.name()));
    this.scenarioType = scenarioType;
    this.requestSchemaValidator = requestSchemaValidator;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your application POST one or more Port Call Events to its synthetic counterpart running in the sandbox";
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
        return Stream.of(
            new UrlPathCheck(PortCallRole::isPublisher, getMatchedExchangeUuid(), "/events"),
            new ResponseStatusCheck(PortCallRole::isSubscriber, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                PortCallRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new ApiHeaderCheck(
                PortCallRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new JsonSchemaCheck(
                PortCallRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new ApiHeaderCheck(
                PortCallRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            PortCallChecks.getPortCallPostPayloadChecks(
                getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
      }
    };
  }
}
