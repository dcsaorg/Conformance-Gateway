package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;

@Slf4j
public class JitOOBTimestampAction extends JitAction {
  private final JitTimestampType timestampType;

  public JitOOBTimestampAction(
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
            : "Receive Out-of-Band %s".formatted(timestampType));
    this.timestampType = timestampType;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownFile("prompt-process-oob-request.md").formatted(timestampType);
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();

    // The timestamp is not provided by the system of the other party, so need to create it here.
    if (timestampType == JitTimestampType.ESTIMATED) {
      dsp =
          dsp.withCurrentTimestamp(
              JitTimestamp.getTimestampForType(JitTimestampType.ESTIMATED, null, dsp.isFYI())
                  .withPortCallServiceID(dsp.portCallServiceID()));
    }

    jsonNode.put("timestampType", timestampType.name());
    return jsonNode;
  }

  @Override
  public boolean isMissingMatchedExchange() {
    return false;
  }
}
