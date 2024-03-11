package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Getter
@Slf4j
public class JitGetEventsAction extends JitAction {
  private final JsonSchemaValidator responseSchemaValidator;

  public JitGetEventsAction(
      String subscriberPartyName,
      String publisherPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetEvents", 200);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET events request with the following parameters: "
        + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(JitRole::isSubscriber, getMatchedExchangeUuid(), "/events"),
            new ResponseStatusCheck(JitRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new JsonSchemaCheck(
                JitRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}
