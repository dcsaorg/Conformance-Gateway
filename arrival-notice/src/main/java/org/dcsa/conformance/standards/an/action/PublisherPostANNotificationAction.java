package org.dcsa.conformance.standards.an.action;

import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.dcsa.conformance.standards.an.party.ANRole;

public class PublisherPostANNotificationAction extends ANAction {

  private final JsonSchemaValidator requestSchemaValidator;

  public PublisherPostANNotificationAction(
      String sourcePartyName,
      String targetPartyName,
      ANAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "POST AN Notification");
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your application POST one or more Arrival Notice notifications to its synthetic counterpart running in the sandbox";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                ANRole::isPublisher, getMatchedExchangeUuid(), "/arrival-notice-notifications"),
            new ResponseStatusCheck(ANRole::isSubscriber, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new ApiHeaderCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new JsonSchemaCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            ANChecks.getANNPostPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }

}
