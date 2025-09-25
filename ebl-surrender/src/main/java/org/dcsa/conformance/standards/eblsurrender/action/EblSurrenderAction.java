package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

@Getter
public abstract class EblSurrenderAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;

  protected EblSurrenderAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
  }

  public abstract Supplier<String> getSrrSupplier();

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction::getSuppliedScenarioParameters
        : _getSspSupplier(previousAction.getPreviousAction());
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }

  public static String getMarkdownHumanReadablePrompt(
      Map<String, String> replacements, String... fileNames) {
    return Arrays.stream(fileNames)
        .map(
            fileName ->
                IOToolkit.templateFileToText(
                    "/standards/eblsurrender/instructions/" + fileName, replacements))
        .collect(Collectors.joining());
  }
}
