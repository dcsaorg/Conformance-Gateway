package org.dcsa.conformance.end.action;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;

public class EndorsementChainAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;

  protected EndorsementChainAction(String sourcePartyName, String targetPartyName, ConformanceAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
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
}
