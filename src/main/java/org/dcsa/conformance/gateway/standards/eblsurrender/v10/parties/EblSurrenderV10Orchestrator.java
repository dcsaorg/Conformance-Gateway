package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.BiConsumer;
import org.dcsa.conformance.gateway.parties.ConformanceOrchestrator;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.*;

public class EblSurrenderV10Orchestrator extends ConformanceOrchestrator {
  private final String platformPartyName;
  private final String carrierPartyName;

  public EblSurrenderV10Orchestrator(String platformPartyName, String carrierPartyName, BiConsumer<String, JsonNode> partyNotifier) {
    super(partyNotifier);
    this.platformPartyName = platformPartyName;
    this.carrierPartyName = carrierPartyName;
  }

  @Override
  protected void initializeScenarios() {
    scenarios.clear();
    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);
      RequestSurrenderForDeliveryAction requestSurrenderForDeliveryAction =
          new RequestSurrenderForDeliveryAction(
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      AcceptSurrenderForDeliveryAction acceptSurrenderForDeliveryAction =
          new AcceptSurrenderForDeliveryAction(
              requestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);
      scenarios.add(
          new EblSurrenderV10Scenario(
              supplyAvailableEblAction,
              requestSurrenderForDeliveryAction,
              acceptSurrenderForDeliveryAction));
    }

    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);

      RequestSurrenderForDeliveryAction firstRequestSurrenderForDeliveryAction =
          new RequestSurrenderForDeliveryAction(
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      RejectSurrenderForDeliveryAction rejectSurrenderForDeliveryAction =
          new RejectSurrenderForDeliveryAction(
              firstRequestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);

      RequestSurrenderForDeliveryAction secondRequestSurrenderForDeliveryAction =
          new RequestSurrenderForDeliveryAction(
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      AcceptSurrenderForDeliveryAction acceptSurrenderForDeliveryAction =
          new AcceptSurrenderForDeliveryAction(
              secondRequestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);

      scenarios.add(
          new EblSurrenderV10Scenario(
              supplyAvailableEblAction,
              firstRequestSurrenderForDeliveryAction,
              rejectSurrenderForDeliveryAction,
              secondRequestSurrenderForDeliveryAction,
              acceptSurrenderForDeliveryAction));
    }
  }
}
