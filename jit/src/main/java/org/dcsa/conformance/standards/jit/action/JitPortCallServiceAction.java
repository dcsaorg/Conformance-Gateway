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
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
@ToString
public class JitPortCallServiceAction extends JitAction {
  public static final String SERVICE_TYPE = "serviceType";

  private final JsonSchemaValidator validator;
  private final PortCallServiceType serviceType;

  public JitPortCallServiceAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      PortCallServiceType serviceType) {
    super(
        context.providerPartyName(),
        context.consumerPartyName(),
        previousAction,
        serviceType != null ? "Port Call Service: " + serviceType.name() : "Port Call Service");
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.PORT_CALL_SERVICE);
    if (serviceType == null && previousAction instanceof JitPortCallServiceAction && dsp != null) {
      serviceType = dsp.portCallServiceType();
    }
    this.serviceType = serviceType;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    dsp = ((JitAction) previousAction).getDsp();
    jsonNode.set("dsp", dsp.toJson());
    if (serviceType != null) jsonNode.put(SERVICE_TYPE, serviceType.name());
    return jsonNode;
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
    dsp =
        dsp.withTerminalCallID(requestJsonNode.path("terminalCallID").asText(null))
            .withPortCallServiceID(requestJsonNode.path("portCallServiceID").asText(null))
            .withPortCallServiceType(
                PortCallServiceType.fromName(
                    requestJsonNode.path("portCallServiceType").asText(null)));
  }

  @Override
  public String getHumanReadablePrompt() {
    if (dsp == null) dsp = ((JitAction) previousAction).getDsp();
    return switch (dsp.selector()) {
      case FULL_ERP:
        yield "Send a Port Call Service (PUT) for Full ERP negotiation";
      case S_A_PATTERN:
        yield "Send a Port Call Service (PUT) for the 'S-A' pattern";
      case GIVEN:
        yield "Send a Port Call Service (PUT) for %s".formatted(serviceType.name());
    };
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
                JitRole::isProvider, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator),
            JitChecks.createChecksForPortCallService(
                JitRole::isProvider, getMatchedExchangeUuid(), expectedApiVersion, serviceType, dsp));
      }
    };
  }
}
