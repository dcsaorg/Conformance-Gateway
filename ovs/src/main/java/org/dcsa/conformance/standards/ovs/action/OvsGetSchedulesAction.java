package org.dcsa.conformance.standards.ovs.action;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.checks.OvsChecks;
import org.dcsa.conformance.standards.ovs.checks.QueryParameterSchemaCheck;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Getter
@Slf4j
public class OvsGetSchedulesAction extends OvsAction {
  private final JsonSchemaValidator responseSchemaValidator;

  public OvsGetSchedulesAction(
      String subscriberPartyName,
      String publisherPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetSchedules", 200);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET schedules request with the following parameters: "
        + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(OvsRole::isSubscriber, getMatchedExchangeUuid(), "/service-schedules"),
          new ApiHeaderCheck(
            OvsRole::isSubscriber,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            OvsRole::isPublisher,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ResponseStatusCheck(OvsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
          new QueryParameterSchemaCheck(
            "",
            "The query parameters of the HTTP request are correct",
            OvsRole::isSubscriber,
            getMatchedExchangeUuid(),
            "/standards/ovs/schemas/ovs-300-publisher.json"),
          new JsonSchemaCheck(
              OvsRole::isPublisher,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              responseSchemaValidator),
          OvsChecks.responseContentChecks(getMatchedExchangeUuid(), expectedApiVersion, sspSupplier)
        );
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}
