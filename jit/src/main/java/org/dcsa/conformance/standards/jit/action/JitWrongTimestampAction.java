package org.dcsa.conformance.standards.jit.action;

import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
public class JitWrongTimestampAction extends JitAction {
  private final JsonSchemaValidator validator;
  private final boolean handledByProvider;

  public JitWrongTimestampAction(
      JitScenarioContext context, ConformanceAction previousAction, boolean handledByProvider) {
    super(
        handledByProvider ? context.consumerPartyName() : context.providerPartyName(),
        handledByProvider ? context.providerPartyName() : context.consumerPartyName(),
        previousAction,
        "Respond to unexpected timestamp (%s party only)"
            .formatted(handledByProvider ? "Provider" : "Consumer"));
    this.handledByProvider = handledByProvider;
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.ERROR_RESPONSE);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a Timestamp request, with an unexpected portCallServiceID. Not to be performed by implementers";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (!handledByProvider) {
          return Stream.of(
              new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.PUT),
              new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 400),
              new ApiHeaderCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  expectedApiVersion),
              new JsonSchemaCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  validator));
        }
        // Service Consumer sends requested timestamp
        return Stream.of(
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.PUT),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 400),
            new ApiHeaderCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                validator));
      }
    };
  }
}
