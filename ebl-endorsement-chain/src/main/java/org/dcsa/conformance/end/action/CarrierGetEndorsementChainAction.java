package org.dcsa.conformance.end.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.end.party.EndorsementChainRole;

public class CarrierGetEndorsementChainAction extends EndorsementChainAction{

  private final JsonSchemaValidator responseSchemaValidator;

  public CarrierGetEndorsementChainAction(
      String providerPartyName,
      String carrierPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(carrierPartyName, providerPartyName, previousAction, "GetEndorsementChain");
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EndorsementChainRole::isCarrier,
                getMatchedExchangeUuid(),
                "/endorsement-chains/{transportDocumentReference}"),
            new ResponseStatusCheck(
                EndorsementChainRole::isProvider, getMatchedExchangeUuid(), 200),
            new JsonSchemaCheck(
                EndorsementChainRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                EndorsementChainRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EndorsementChainRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion));
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonActionNode =
        super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    return jsonActionNode;
  }
}
