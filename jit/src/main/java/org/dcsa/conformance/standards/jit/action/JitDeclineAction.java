package org.dcsa.conformance.standards.jit.action;

import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
public class JitDeclineAction extends JitAction {
  private final JsonSchemaValidator validator;
  private final boolean sendByProvider;

  public JitDeclineAction(
      JitScenarioContext context, ConformanceAction previousAction, boolean sendByProvider) {
    super(
        sendByProvider ? context.providerPartyName() : context.consumerPartyName(),
        sendByProvider ? context.consumerPartyName() : context.providerPartyName(),
        previousAction,
        "Decline Port Call Service");
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.DECLINE);
    this.sendByProvider = sendByProvider;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a %s (POST) request".formatted(getActionTitle());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (!sendByProvider) {
          return Stream.of(
              new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.POST),
              new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 204),
              new JsonSchemaCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  validator));
        }
        return Stream.of(
            new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.POST),
            new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
            JitChecks.checkIsFYIIsCorrect(
                JitRole::isProvider, getMatchedExchangeUuid(), expectedApiVersion, dsp),
            new JsonSchemaCheck(
                JitRole::isProvider, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator));
      }
    };
  }
}
