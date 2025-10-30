package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.portcall.JitScenarioContext;

@Slf4j
@ToString
public class PortCallPostAction extends PortCallAction {
  public static final String SERVICE_TYPE = "serviceTypeCode";

  private final JsonSchemaValidator requestSchemaValidator;

  public PortCallPostAction(
      JitScenarioContext context,
      String publisherPartyName,
      String subscriberPartyName,
      PortCallAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {

    super(publisherPartyName, subscriberPartyName, previousAction, "POST Port Call");
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Have your application POST one or more events to its synthetic counterpart running in the sandbox";
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(ANRole::isPublisher, getMatchedExchangeUuid(), "/arrival-notices"),
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
            ANChecks.getANPostPayloadChecks(
                getMatchedExchangeUuid(), expectedApiVersion, scenarioType.name()));
      }
    };
  }
}
