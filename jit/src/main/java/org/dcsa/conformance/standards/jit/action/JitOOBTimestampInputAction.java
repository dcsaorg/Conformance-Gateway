package org.dcsa.conformance.standards.jit.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;

@Slf4j
public class JitOOBTimestampInputAction extends JitAction {

  private final JitTimestampType timestampType;

  public JitOOBTimestampInputAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      JitTimestampType timestampType,
      boolean sendByProvider) {
    super(
        sendByProvider ? context.providerPartyName() : context.consumerPartyName(),
        sendByProvider ? context.consumerPartyName() : context.providerPartyName(),
        previousAction,
        sendByProvider
            ? "Send Out-of-Band %s".formatted(timestampType)
            : "Receive Out-of-Band: %s".formatted(timestampType));
    this.timestampType = timestampType;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("timestampType", timestampType.name());
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Process an Out-of-Band request for a %s timestamp and supply the used timestamp:"
        .formatted(timestampType));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    if (dsp == null) dsp = ((JitAction) previousAction).dsp;
    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(
            timestampType, ((JitAction) previousAction).dsp.currentTimestamp(), dsp.isFYI());
    return OBJECT_MAPPER.createObjectNode().put("timestamp", timestamp.dateTime());
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    log.info("JitOOBTimestampInputAction.handlePartyInput({})", partyInput.toPrettyString());
    super.handlePartyInput(partyInput);
    if (dsp == null) dsp = ((JitAction) previousAction).dsp;

    // Store supplied timestamp into the DSP
    dsp =
        dsp.withPreviousTimestamp(dsp.currentTimestamp())
            .withCurrentTimestamp(
                dsp.currentTimestamp()
                    .withDateTime(partyInput.get("input").get("timestamp").asText()));
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }
}
