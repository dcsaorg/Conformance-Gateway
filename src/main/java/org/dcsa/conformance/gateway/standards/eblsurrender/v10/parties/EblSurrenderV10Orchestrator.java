package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import org.dcsa.conformance.gateway.parties.ConformanceOrchestrator;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.*;

public class EblSurrenderV10Orchestrator extends ConformanceOrchestrator {
  private final String platformPartyName;
  private final String carrierPartyName;

  public EblSurrenderV10Orchestrator(String platformPartyName, String carrierPartyName) {
    this.platformPartyName = platformPartyName;
    this.carrierPartyName = carrierPartyName;
  }

  @Override
  protected void initializeScenarios() {
    scenarios.clear();
    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);
      RequestSurrenderAction requestSurrenderForDeliveryAction =
          new RequestSurrenderAction(false,
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      AcceptSurrenderRequestAction acceptSurrenderRequestAction =
          new AcceptSurrenderRequestAction(
              requestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);
      scenarios.add(
          new EblSurrenderV10Scenario(
              supplyAvailableEblAction,
              requestSurrenderForDeliveryAction,
              acceptSurrenderRequestAction));
    }

    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);

      RequestSurrenderAction firstRequestSurrenderForDeliveryAction =
          new RequestSurrenderAction(false,
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      RejectSurrenderRequestAction rejectSurrenderRequestAction =
          new RejectSurrenderRequestAction(
              firstRequestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);

      RequestSurrenderAction secondRequestSurrenderForDeliveryAction =
          new RequestSurrenderAction(false,
              supplyAvailableEblAction.getTdrSupplier(), platformPartyName, carrierPartyName);
      AcceptSurrenderRequestAction acceptSurrenderRequestAction =
          new AcceptSurrenderRequestAction(
              secondRequestSurrenderForDeliveryAction.getSrrSupplier(),
              supplyAvailableEblAction.getTdrSupplier(),
                  carrierPartyName,
                  platformPartyName);

      scenarios.add(
          new EblSurrenderV10Scenario(
              supplyAvailableEblAction,
              firstRequestSurrenderForDeliveryAction,
              rejectSurrenderRequestAction,
              secondRequestSurrenderForDeliveryAction,
              acceptSurrenderRequestAction));
    }
  }
}
