package org.dcsa.conformance.standards.eblissuance.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.standards.eblissuance.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class IssuanceAction extends ConformanceAction {

  protected final int expectedStatus;

  protected IssuanceAction(
    String sourcePartyName,
    String targetPartyName,
    IssuanceAction previousAction,
    String actionTitle,
    int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
  }

  protected IssuanceAction getPreviousIssuanceAction() {
    return (IssuanceAction) previousAction;
  }

  protected String getMarkdownHumanReadablePrompt(
    Map<String, String> replacements, String... fileNames) {
    return Arrays.stream(fileNames)
      .map(
        fileName ->
          IOToolkit.templateFileToText(
            "/standards/eblissuance/instructions/" + fileName, replacements))
      .collect(Collectors.joining());
  }

  protected Consumer<SuppliedScenarioParameters> getSspConsumer() {
    return getPreviousIssuanceAction().getSspConsumer();
  }

  protected Supplier<SuppliedScenarioParameters> getSspSupplier() {
    return getPreviousIssuanceAction().getSspSupplier();
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousIssuanceAction().getCspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousIssuanceAction().getCspSupplier();
  }

  protected abstract Supplier<String> getTdrSupplier();

}
