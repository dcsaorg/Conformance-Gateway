package org.dcsa.conformance.standards.jit.action;

import java.util.stream.Stream;
import lombok.ToString;
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
@ToString
public class JitOmitTerminalCallAction extends JitAction {
  private final JsonSchemaValidator validator;

  public JitOmitTerminalCallAction(JitScenarioContext context, ConformanceAction previousAction) {
    super(
        context.providerPartyName(),
        context.consumerPartyName(),
        previousAction,
        "Omit Terminal Call");
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.OMIT_PORT_CALL);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send an %s (POST) request".formatted(getActionTitle());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
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
