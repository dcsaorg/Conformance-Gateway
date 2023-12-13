package org.dcsa.conformance.standards.ovs.action;

import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Getter
@Slf4j
public class OvsGetSchedulesAction extends OvsAction {
  private final JsonSchemaValidator responseSchemaValidator;

  public OvsGetSchedulesAction(
      String subscriberPartyName,
      String publisherPartyName,
      OvsAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetSchedules", 200);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET schedules request";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(OvsRole::isSubscriber, getMatchedExchangeUuid(), "/service-schedules"),
            new ResponseStatusCheck(OvsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new JsonSchemaCheck(
                OvsRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }
}
