package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.cs.checks.CsChecks;
import org.dcsa.conformance.standards.cs.party.CsRole;

public class CsGetRoutingsAction extends CsAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public CsGetRoutingsAction(
      String subscriberPartyName,
      String publisherPartyName,
      CsAction previousAction,
      JsonSchemaValidator responseSchemaValidator1) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetRoutings", 200);

    this.responseSchemaValidator = responseSchemaValidator1;
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    ObjectNode jsonActionNode =
        super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    String cursor = dsp.cursor();
    if (cursor != null && !cursor.isEmpty()) {
      jsonActionNode.put("cursor", cursor);
    }
    return jsonActionNode;
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
            new UrlPathCheck(
                CsRole::isSubscriber, getMatchedExchangeUuid(), "/point-to-point-routes"),
            new ResponseStatusCheck(CsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new JsonSchemaCheck(
                CsRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                CsRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                CsRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            CsChecks.getPayloadChecksForPtp(
                getMatchedExchangeUuid(),
                expectedApiVersion,
                sspSupplier,
                getDspSupplier(),
                previousAction instanceof CsGetRoutingsAction));
      }
    };
  }
}
