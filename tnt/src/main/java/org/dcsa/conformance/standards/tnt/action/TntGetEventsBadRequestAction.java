package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@Getter
@Slf4j
public class TntGetEventsBadRequestAction extends TntAction {
  private final Map<TntEventType, JsonSchemaValidator> eventSchemaValidators;

  public TntGetEventsBadRequestAction(
    String subscriberPartyName,
    String publisherPartyName,
    TntAction previousAction,
    Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetEvents (Bad Request)", 400);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET events bad request with the following parameters: "
      + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(TntRole::isSubscriber, getMatchedExchangeUuid(), "/events"),
          new ResponseStatusCheck(TntRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
          new ApiHeaderCheck(
            TntRole::isSubscriber,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new ApiHeaderCheck(
            TntRole::isPublisher,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ResponseStatusCheck(TntRole::isPublisher, getMatchedExchangeUuid(), 400)
        );
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}
