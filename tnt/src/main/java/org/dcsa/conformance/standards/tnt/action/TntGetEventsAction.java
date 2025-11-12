package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.checks.TntChecks;
import org.dcsa.conformance.standards.tnt.checks.TntSchemaConformanceCheck;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@Getter
@Slf4j
public class TntGetEventsAction extends TntAction {
  private final Map<TntEventType, JsonSchemaValidator> eventSchemaValidators;

  public TntGetEventsAction(
      String subscriberPartyName,
      String publisherPartyName,
      TntAction previousAction,
      Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(subscriberPartyName,
      publisherPartyName,
      previousAction,
      (previousAction instanceof TntGetEventsAction)
        ? "GetEvents (Next page)"
        : "GetEvents", 200);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  @Override
  public String getHumanReadablePrompt() {
    return previousAction instanceof TntGetEventsAction
      ? "Send a GET events request to fetch the next results page, using the cursor retrieved from the headers of the response of the first GET request."
      : "Send a GET events request with the following parameters: "
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
            new TntSchemaConformanceCheck(getMatchedExchangeUuid(), eventSchemaValidators),
            TntChecks.responseContentChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }

  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode = super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
  }
}
