package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import lombok.AllArgsConstructor;
import org.dcsa.conformance.gateway.parties.ConformanceOrchestrator;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.*;

import java.util.UUID;

@AllArgsConstructor
public class EblSurrenderV10Orchestrator extends ConformanceOrchestrator {
  private final String platformPartyName;
  private final String carrierPartyName;

  @Override
  protected void initializeScenarios() {
    scenarios.clear();
    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);

      RequestSurrenderAction requestSurrenderForDeliveryAction =
          new RequestSurrenderAction(
              UUID.randomUUID().toString(),
              false,
              supplyAvailableEblAction.getTdrSupplier(),
              platformPartyName,
              carrierPartyName);

      AcceptSurrenderRequestAction acceptSurrenderRequestAction =
          new AcceptSurrenderRequestAction(
              requestSurrenderForDeliveryAction.getSrr(),
              supplyAvailableEblAction.getTdrSupplier(),
              carrierPartyName,
              platformPartyName);

      scenarios.add(
          new EblSurrenderV10Scenario(
              supplyAvailableEblAction,
              requestSurrenderForDeliveryAction,
              acceptSurrenderRequestAction));
    }

    // TODO bring this back
    if (System.currentTimeMillis() < 0)
    { // scoped
      SupplyAvailableTdrAction supplyAvailableEblAction =
          new SupplyAvailableTdrAction(carrierPartyName);

      RequestSurrenderAction firstRequestSurrenderForDeliveryAction =
          new RequestSurrenderAction(
              UUID.randomUUID().toString(),
              false,
              supplyAvailableEblAction.getTdrSupplier(),
              platformPartyName,
              carrierPartyName);

      RejectSurrenderRequestAction rejectSurrenderRequestAction =
          new RejectSurrenderRequestAction(
              firstRequestSurrenderForDeliveryAction.getSrr(),
              supplyAvailableEblAction.getTdrSupplier(),
              carrierPartyName,
              platformPartyName);

      RequestSurrenderAction secondRequestSurrenderForDeliveryAction =
          new RequestSurrenderAction(
              UUID.randomUUID().toString(),
              false,
              supplyAvailableEblAction.getTdrSupplier(),
              platformPartyName,
              carrierPartyName);

      AcceptSurrenderRequestAction acceptSurrenderRequestAction =
          new AcceptSurrenderRequestAction(
              secondRequestSurrenderForDeliveryAction.getSrr(),
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
