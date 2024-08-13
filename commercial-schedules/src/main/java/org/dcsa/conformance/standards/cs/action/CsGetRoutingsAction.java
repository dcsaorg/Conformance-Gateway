package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.party.CsRole;
import org.dcsa.conformance.standards.cs.checks.CsChecks;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Stream;

public class CsGetRoutingsAction extends CsAction{

  private final JsonSchemaValidator responseSchemaValidator;
    public CsGetRoutingsAction(String subscriberPartyName, String publisherPartyName, ConformanceAction previousAction, JsonSchemaValidator responseSchemaValidator1) {
      super(subscriberPartyName, publisherPartyName, previousAction, "GetRoutings", 200);

        this.responseSchemaValidator = responseSchemaValidator1;
    }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET point to point routings request with the following parameters: "
      + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(CsRole::isSubscriber, getMatchedExchangeUuid(), "/point-to-point-routes"),
          new ResponseStatusCheck(CsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
          new JsonSchemaCheck(
            CsRole::isPublisher,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
          CsChecks.getPayloadChecksForPtp(getMatchedExchangeUuid(),expectedApiVersion));
      }
    };
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }


}
