package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.standards.eblsurrender.party.SuppliedScenarioParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public abstract class EblSurrenderAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  private final int expectedStatus;

  protected EblSurrenderAction(
    String sourcePartyName,
    String targetPartyName,
    int expectedStatus,
    ConformanceAction previousAction,
    String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
  }

  protected int getExpectedStatus() {
    return expectedStatus;
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    if (previousAction == null) {
      SuppliedScenarioParameters parameters = createSyntheticScenarioParameters();
      return () -> parameters;
    }
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
      ? supplyAvailableTdrAction::getSuppliedScenarioParameters
      : _getSspSupplier(previousAction.getPreviousAction());
  }

  private static SuppliedScenarioParameters createSyntheticScenarioParameters() {
    var carrierParty = createSyntheticParty("Synthetic Carrier", "WAVE");
    var issueToParty = createSyntheticParty("Issue To Party", "WAVE");
    var surrendereeParty = createSyntheticParty("Surrenderee Party", "BOLE");
    return new SuppliedScenarioParameters(ReferenceGenerator.newReference(), issueToParty, carrierParty, surrendereeParty);
  }

  private static ObjectNode createSyntheticParty(String partyName, String eblPlatform) {
    ObjectNode party = OBJECT_MAPPER.createObjectNode()
      .put("partyName", partyName)
      .put("eblPlatform", eblPlatform);

    party.putArray("identifyingCodes")
      .addObject()
      .put("codeListProvider", "DCSA")
      .put("partyCode", "SYNTHETIC");
    return party;
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }

  public static String getMarkdownHumanReadablePrompt(Map<String, String> replacements, String... fileNames) {
    return Arrays.stream(fileNames)
      .map(fileName -> IOToolkit.templateFileToText("/standards/eblsurrender/instructions/" + fileName, replacements))
      .collect(Collectors.joining());
  }
}
