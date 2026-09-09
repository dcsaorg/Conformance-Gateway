package org.dcsa.conformance.end.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.end.party.DynamicScenarioParameters;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EndorsementChainAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected EndorsementChainAction(
    String sourcePartyName,
    String targetPartyName,
    EndorsementChainAction previousAction,
    String actionTitle) {
    this(sourcePartyName, targetPartyName, previousAction, actionTitle, null);
  }

  protected EndorsementChainAction(
    String sourcePartyName,
    String targetPartyName,
    EndorsementChainAction previousAction,
    String actionTitle,
    SuppliedScenarioParameters standaloneScenarioParameters) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier =
      standaloneScenarioParameters == null
        ? _getSspSupplier(previousAction)
        : () -> standaloneScenarioParameters;
    this.dsp =
      previousAction == null
        ? new OverwritingReference<>(null, new DynamicScenarioParameters(null))
        : new OverwritingReference<>(previousAction.dsp, null);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  protected String getMarkdownHumanReadablePrompt(
    Map<String, String> replacementsMap, String... fileNames) {

    return Arrays.stream(fileNames)
      .map(
        fileName ->
          IOToolkit.templateFileToText(
            "/standards/end/instructions/" + fileName, replacementsMap))
      .collect(Collectors.joining());
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction
      instanceof SupplyScenarioParametersAction supplyScenarioParametersActionAction
      ? supplyScenarioParametersActionAction::getSuppliedScenarioParameters
      : previousAction == null
      ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
      : _getSspSupplier(previousAction.getPreviousAction());
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    DynamicScenarioParameters dspReference = getDspSupplier().get();

    var updatedDsp = dspReference;

    String[] urlStrings = exchange.getRequest().url().split("/");
    String tdr = urlStrings[urlStrings.length - 1];
    if (tdr != null) {
      updatedDsp = getDspSupplier().get().withTransportDocumentReference(tdr);
    }
    if (!dspReference.equals(updatedDsp)) {
      dsp.set(updatedDsp);
    }
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null));
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
