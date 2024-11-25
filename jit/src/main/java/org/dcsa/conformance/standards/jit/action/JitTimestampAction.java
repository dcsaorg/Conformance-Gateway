package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
public class JitTimestampAction extends JitAction {
  private final JitTimestampType timestampType;
  private final JsonSchemaValidator responseSchemaValidator;
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
    responseSchemaValidator =
        context.componentFactory().getMessageSchemaValidator(timestampType.getJitSchema());
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    log.info(
        "JitTimestampAction.doHandleExchange() requestJsonNode: {}",
        requestJsonNode.toPrettyString());

    // TODO: update when type of timestamps is PUT through single enpoint
    JitTimestamp receivedTimestamp = JitTimestamp.fromJson(requestJsonNode);
    if (dsp.previousTimestamp() == null) {
      dsp = dsp.withPreviousTimestamp(receivedTimestamp);
    }

    // Update DSP with the timestamp type
    String[] urlPath = exchange.getRequest().url().split("/");
    if (urlPath[urlPath.length - 1].equals("requested-timestamp")) {
      dsp = dsp.withTimestampType(JitTimestampType.REQUESTED);
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    //    jsonNode.set(
    //        SupplyScenarioParametersAction.PARAMETERS,
    //        ((SupplyScenarioParametersAction) previousAction.getPreviousAction())
    //            .getSuppliedScenarioParameters()
    //            .toJson());
    jsonNode.put("timestampType", timestampType.name());
    if (((JitAction) previousAction).getDsp() != null) {
      jsonNode.put("portCallServiceID", ((JitAction) previousAction).getDsp().portCallServiceID());
      dsp = ((JitAction) previousAction).getDsp();
    }
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
//        ActionCheck checksForTimestamp =
//            JitChecks.createChecksForTimestamp(
//                JitRole::isProvider, getMatchedExchangeUuid(), expectedApiVersion, dsp);
        if (sendByProvider) {
          return Stream.of(
              new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), "PUT"),
              new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
              new JsonSchemaCheck(
                  JitRole::isProvider,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  responseSchemaValidator)
//              checksForTimestamp
              );
        }
        // Consumer sends requested timestamp
        return Stream.of(
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), "PUT"),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                JitRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                responseSchemaValidator)
//            checksForTimestamp
            );
      }
    };
  }
}
