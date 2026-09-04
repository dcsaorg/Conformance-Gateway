package org.dcsa.conformance.standards.an.action;

import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANRole;

import java.util.stream.Stream;

public class PublisherPostANNotificationAction extends ANAction {

  private final JsonSchemaValidator requestSchemaValidator;

  public PublisherPostANNotificationAction(
    String sourcePartyName,
    String targetPartyName,
    ANAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    String title) {
    super(sourcePartyName, targetPartyName, previousAction, title);
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
            ANRole::isProducer, getMatchedExchangeUuid(), "/arrival-notice-notifications"),
          new ResponseStatusCheck(ANRole::isConsumer, getMatchedExchangeUuid(), 200),
          new ApiHeaderCheck(
            ANRole::isConsumer,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ApiHeaderCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion),
          new JsonSchemaCheck(
            ANRole::isProducer,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            requestSchemaValidator));
      }
    };
  }

}
