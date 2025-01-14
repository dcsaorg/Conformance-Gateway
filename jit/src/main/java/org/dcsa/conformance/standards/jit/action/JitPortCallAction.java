package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
@ToString
public class JitPortCallAction extends JitAction {
  private final JsonSchemaValidator validator;

  public JitPortCallAction(JitScenarioContext context, ConformanceAction previousAction) {
    super(context.providerPartyName(), context.consumerPartyName(), previousAction, "Port Call");
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.PORT_CALL);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    log.info(
        "{}.doHandleExchange() requestJsonNode: {}",
        getClass().getSimpleName(),
        requestJsonNode.toPrettyString());

    // Update DSP with the Port Call Service response from the provider, or create a new one.
    updateDspFromResponse(requestJsonNode);
  }

  private void updateDspFromResponse(JsonNode requestJsonNode) {
    dsp = dsp.withPortCallID(requestJsonNode.path("portCallID").asText(null));
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a Port Call (PUT)";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.PUT),
            new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
            new ApiHeaderCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                JitRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                JitRole::isProvider, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator));
      }
    };
  }
}
