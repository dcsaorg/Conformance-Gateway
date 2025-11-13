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

public class SubscriberGetPortCallEventsAction extends PortCallAction{

  private final JsonSchemaValidator responseSchemaValidator;

  public SubscriberGetPortCallEventsAction(String subscriberPartyName, String publisherPartyName, PortCallAction previousAction, JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GET Port Call Events");
    this.responseSchemaValidator = responseSchemaValidator;
  }


  @Override
  public String getHumanReadablePrompt() {
    return "Have your application GET from its counterpart running in the sandbox the Port Call Events with the any of the following attributes: \n\n"
        + sspSupplier.get().toJson();
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(PortCallRole::isSubscriber, getMatchedExchangeUuid(), "/events"),
          new ResponseStatusCheck(PortCallRole::isPublisher, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            PortCallRole::isPublisher,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          new ApiHeaderCheck(
            PortCallRole::isSubscriber,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            PortCallRole::isPublisher,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          PortCallChecks.getGetResponsePayloadChecks(getMatchedExchangeUuid(),expectedApiVersion));
      }
    };
  }
}
