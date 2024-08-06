package org.dcsa.conformance.standards.cs.action;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SupplyScenarioParametersAction extends ConformanceAction {

  private final LinkedHashSet<CsFilterParameter> csFilterParameters;

  public SupplyScenarioParametersAction(
    String publisherPartyName, CsFilterParameter... csFilterParameters) {
    super(
      publisherPartyName,
      null,
      null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          Arrays.stream(csFilterParameters)
            .map(CsFilterParameter::getQueryParamName)
            .collect(Collectors.joining(", "))));
    this.csFilterParameters =
      Stream.of(csFilterParameters).collect(Collectors.toCollection(LinkedHashSet::new));
  }


  @Override
  public String getHumanReadablePrompt() {
    return null;
  }
}
