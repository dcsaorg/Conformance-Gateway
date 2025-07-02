package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANRole;

public class SubscriberGetANAction extends AnAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public SubscriberGetANAction(
      String sourcePartyName,
      String targetPartyName,
      AnAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "GET AN");
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the arrival notices";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(ANRole::isSubscriber, getMatchedExchangeUuid(), "/arrival-notices"),
            new ResponseStatusCheck(ANRole::isPublisher, getMatchedExchangeUuid(), 200),
            new JsonSchemaCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion));
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode();
  }
}
