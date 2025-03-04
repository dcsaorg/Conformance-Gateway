package org.dcsa.conformance.standards.jit.action;

import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
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
  private final boolean sendByProvider;

  public JitWrongTimestampAction(
      JitScenarioContext context, ConformanceAction previousAction, boolean sendByProvider) {
    super(
        sendByProvider ? context.providerPartyName() : context.consumerPartyName(),
        sendByProvider ? context.consumerPartyName() : context.providerPartyName(),
        previousAction,
        "Respond to wrong timestamp (%s party only)"
            .formatted(sendByProvider ? "Consumer" : "Provider"));
    this.sendByProvider = sendByProvider;
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.ERROR_RESPONSE);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a Timestamp request, with an unknown portCallServiceID. Not to be performed by implementers";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (sendByProvider) {
          return Stream.of(
              new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.PUT),
              new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 404),
              new JsonSchemaCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  validator));
        }
        // Service Consumer sends requested timestamp
        return Stream.of(
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.PUT),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 404),
            new JsonSchemaCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                validator));
      }
    };
  }
}
