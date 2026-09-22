package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ANAction extends ConformanceAction {

  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected ANAction(
    String sourcePartyName, String targetPartyName, ANAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.dsp =
      previousAction == null
        ? new OverwritingReference<>(
        null, new DynamicScenarioParameters(null, null, null, null, null))
        : new OverwritingReference<>(previousAction.dsp, null);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null, null, null, null, null));
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    if (this instanceof SubscriberGetANAction) {
      updatePaginationState(exchange);
    }
  }

  private void updatePaginationState(ConformanceExchange exchange) {
    DynamicScenarioParameters current = getDspSupplier().get();
    DynamicScenarioParameters updated = current;
    var cursorValues = exchange.getResponse().message().headers().get("Next-Page-Cursor");
    if (cursorValues != null && !cursorValues.isEmpty()) {
      updated = updated.withCursor(cursorValues.iterator().next());
    }
    String pageHash = hash(exchange.getResponse().message().body().toString());
    updated =
      previousAction instanceof SubscriberGetANAction
        ? updated.withSecondPageHash(pageHash)
        : updated.withFirstPageHash(pageHash);
    if (!current.equals(updated)) {
      dsp.set(updated);
    }
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
        .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp.hasCurrentValue()) {
      jsonState.set("currentDsp", dsp.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      dsp.set(DynamicScenarioParameters.fromJson(dspNode));
    }
  }
}
