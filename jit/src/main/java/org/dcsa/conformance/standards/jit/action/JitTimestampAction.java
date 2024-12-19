package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
public class JitTimestampAction extends JitAction {
  private final JitTimestampType timestampType;
  private final JsonSchemaValidator validator;
  private final boolean sendByProvider;

  public JitTimestampAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      JitTimestampType timestampType,
      boolean sendByProvider) {
    super(
        sendByProvider ? context.providerPartyName() : context.consumerPartyName(),
        sendByProvider ? context.consumerPartyName() : context.providerPartyName(),
        previousAction,
        sendByProvider
            ? "Send %s".formatted(timestampType)
            : "Receive %s".formatted(timestampType));
    this.timestampType = timestampType;
    this.sendByProvider = sendByProvider;
    validator = context.componentFactory().getMessageSchemaValidator(timestampType.getJitSchema());
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    log.info(
        "{}.doHandleExchange() requestJsonNode: {}",
        getClass().getSimpleName(),
        requestJsonNode.toPrettyString());

    JitTimestamp receivedTimestamp = JitTimestamp.fromJson(requestJsonNode);
    dsp =
        dsp.withPreviousTimestamp(dsp.currentTimestamp())
            .withCurrentTimestamp(receivedTimestamp);
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("timestampType", timestampType.name());
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send %s timestamp (PUT) call".formatted(timestampType);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        ActionCheck checksForTimestamp =
            JitChecks.createChecksForTimestamp(getMatchedExchangeUuid(), expectedApiVersion, dsp);
        if (sendByProvider) {
          return Stream.of(
              new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.PUT),
              new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
              new JsonSchemaCheck(
                  JitRole::isProvider,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  validator),
              checksForTimestamp);
        }
        // Consumer sends requested timestamp
        return Stream.of(
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.PUT),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                JitRole::isConsumer, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator),
            checksForTimestamp);
      }
    };
  }
}
