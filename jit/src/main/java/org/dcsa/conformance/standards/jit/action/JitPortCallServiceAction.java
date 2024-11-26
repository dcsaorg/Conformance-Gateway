package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;
import org.dcsa.conformance.standards.jit.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
@ToString
public class JitPortCallServiceAction extends JitAction {
  public static final String SERVICE_TYPE = "serviceType";

  private final JsonSchemaValidator responseSchemaValidator;
  private final PortCallServiceType serviceType;

  public JitPortCallServiceAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      PortCallServiceType serviceType) {
    super(
        context.providerPartyName(),
        context.consumerPartyName(),
        previousAction,
        "Port Call Service: " + serviceType.name());
    responseSchemaValidator =
        context.componentFactory().getMessageSchemaValidator(JitSchema.PORT_CALL_SERVICE);
    this.serviceType = serviceType;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set(
        SupplyScenarioParametersAction.PARAMETERS,
        ((SupplyScenarioParametersAction) previousAction).getSuppliedScenarioParameters().toJson());
    jsonNode.put(SERVICE_TYPE, serviceType.name());
    return jsonNode;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    log.info(
        "JitPortCallServiceAction.doHandleExchange() requestJsonNode: {}",
        requestJsonNode.toPrettyString());

    // Update DSP with the Port Call Service response from the provider
    dsp =
        new DynamicScenarioParameters(
            null,
            null,
            PortCallServiceType.fromName(
                requestJsonNode.get("specification").get("portCallServiceType").asText()),
            null,
            requestJsonNode.get("portCall").get("portCallID").asText(),
            requestJsonNode.get("terminalCall").get("terminalCallID").asText(),
            requestJsonNode.get("specification").get("portCallServiceID").asText());
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a Port Call Service (PUT) for %s".formatted(serviceType.name());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), "PUT"),
            new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                responseSchemaValidator),
            JitChecks.createChecksForPortCallService(
                JitRole::isProvider, getMatchedExchangeUuid(), expectedApiVersion, serviceType));
      }
    };
  }
}
