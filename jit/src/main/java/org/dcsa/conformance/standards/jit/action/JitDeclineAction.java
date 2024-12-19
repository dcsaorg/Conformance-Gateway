package org.dcsa.conformance.standards.jit.action;

import java.util.stream.Stream;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
@ToString
public class JitDeclineAction extends JitAction {
  private final JsonSchemaValidator validator;

  public JitDeclineAction(JitScenarioContext context, ConformanceAction previousAction) {
    super(
        context.consumerPartyName(),
        context.providerPartyName(),
        previousAction,
        "Decline Port Call Service");
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.DECLINE);
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
        return Stream.of(
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.POST),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                JitRole::isConsumer, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator));
      }
    };
  }
}
