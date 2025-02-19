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
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
@ToString
public class JitPortCallServiceAction extends JitAction {
  public static final String SERVICE_TYPE = "serviceTypeCode";

  private final JsonSchemaValidator validator;
  private final PortCallServiceTypeCode serviceType;

  public JitPortCallServiceAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      PortCallServiceTypeCode serviceType,
      JitServiceTypeSelector selector) {
    super(
        context.providerPartyName(),
        context.consumerPartyName(),
        previousAction,
        calculateTitle(serviceType, selector));
    validator = context.componentFactory().getMessageSchemaValidator(JitSchema.PORT_CALL_SERVICE);
    if (serviceType == null && previousAction instanceof JitPortCallServiceAction && dsp != null) {
      serviceType = dsp.portCallServiceTypeCode();
    }
    this.serviceType = serviceType;
  }

  private static String calculateTitle(
      PortCallServiceTypeCode serviceType, JitServiceTypeSelector selector) {
    if (serviceType != null) return "Port Call Service(%s)".formatted(serviceType.name());
    return "Port Call Service(%s)".formatted(selector.getFullName());
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
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
        dsp.withPortCallServiceID(requestJsonNode.path(JitChecks.PORT_CALL_SERVICE_ID).asText(null))
            .withPortCallServiceTypeCode(
                PortCallServiceTypeCode.fromName(
                    requestJsonNode.path(JitChecks.PORT_CALL_SERVICE_TYPE).asText(null)));
  }

  @Override
  public String getHumanReadablePrompt() {
    if (dsp == null) dsp = ((JitAction) previousAction).getDsp();
    String replacement =
        switch (dsp.selector()) {
          case FULL_ERP, S_A_PATTERN:
            yield "the %s".formatted(dsp.selector().getFullName());
          case GIVEN:
            yield "%s".formatted(serviceType.name());
          case ANY:
            yield "a service you supply";
        };
    return getMarkdownFile("prompt-send-port-call-service.md").formatted(replacement);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (dsp == null) return Stream.of();
        return Stream.of(
            new UrlPathCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                JitStandard.PORT_CALL_SERVICES_URL + dsp.portCallServiceID()),
            new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.PUT),
            new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                JitRole::isProvider, getMatchedExchangeUuid(), HttpMessageType.REQUEST, validator),
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
            JitChecks.createChecksForPortCallService(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                expectedApiVersion,
                serviceType,
                dsp));
      }
    };
  }
}
