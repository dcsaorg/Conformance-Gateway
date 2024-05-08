package org.dcsa.conformance.standards.an.action;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeRole;

@Getter
@Slf4j
public class ArrivalNoticeGetNotificationAction extends ArrivalNoticeAction {
  private final JsonSchemaValidator responseSchemaValidator;

  public ArrivalNoticeGetNotificationAction(
      String subscriberPartyName,
      String publisherPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetArrivalNotice", 200);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET Arrival Notice request with the following parameters: "
        + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(ArrivalNoticeRole::isNotifyParty, getMatchedExchangeUuid(), "/arrival-notices"),
            new ResponseStatusCheck(ArrivalNoticeRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new JsonSchemaCheck(
                ArrivalNoticeRole::isCarrier,
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
