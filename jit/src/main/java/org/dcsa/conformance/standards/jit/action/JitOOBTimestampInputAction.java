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
      JitTimestampType timestampType) {
    super(
        context.consumerPartyName(),
        context.providerPartyName(),
        previousAction,
        "Receive Out-of-Band: %s".formatted(timestampType));
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
    return ("Supply the out-of-band %s timestamp that you (could) have received out-of-band from the service provider, using the format below:"
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
