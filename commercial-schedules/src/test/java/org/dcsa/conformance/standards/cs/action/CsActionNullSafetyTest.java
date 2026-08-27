package org.dcsa.conformance.standards.cs.action;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.junit.jupiter.api.Test;

class CsActionNullSafetyTest {

  @Test
  void getRoutingsActionDoesNotFailBeforeScenarioParametersAreProvided() {
    SupplyScenarioParametersAction supplyAction =
        new SupplyScenarioParametersAction("publisherPartyName", CsFilterParameter.LIMIT);
    CsGetRoutingsAction action =
        new CsGetRoutingsAction("subscriberPartyName", "publisherPartyName", supplyAction, null);

    assertDoesNotThrow(() -> action.asJsonNode());
    assertDoesNotThrow(() -> action.getHumanReadablePrompt());
    assertDoesNotThrow(() -> action.createCheck("1.0.0").subChecksStream().count());
  }

  @Test
  void getPortSchedulesActionDoesNotFailBeforeScenarioParametersAreProvided() {
    SupplyScenarioParametersAction supplyAction =
        new SupplyScenarioParametersAction("publisherPartyName", CsFilterParameter.LIMIT);
    CsGetPortSchedulesAction action =
        new CsGetPortSchedulesAction("subscriberPartyName", "publisherPartyName", supplyAction, null);

    assertDoesNotThrow(() -> action.asJsonNode());
    assertDoesNotThrow(() -> action.getHumanReadablePrompt());
    assertDoesNotThrow(() -> action.createCheck("1.0.0").subChecksStream().count());
  }

  @Test
  void getVesselSchedulesActionDoesNotFailBeforeScenarioParametersAreProvided() {
    SupplyScenarioParametersAction supplyAction =
        new SupplyScenarioParametersAction("publisherPartyName", CsFilterParameter.LIMIT);
    CsGetVesselSchedulesAction action =
        new CsGetVesselSchedulesAction("subscriberPartyName", "publisherPartyName", supplyAction, null);

    assertDoesNotThrow(() -> action.asJsonNode());
    assertDoesNotThrow(() -> action.getHumanReadablePrompt());
    assertDoesNotThrow(() -> action.createCheck("1.0.0").subChecksStream().count());
  }

  @Test
  void secondPageActionDoesNotFailBeforeFirstPageTrafficExists() {
    SupplyScenarioParametersAction supplyAction =
        new SupplyScenarioParametersAction("publisherPartyName", CsFilterParameter.LIMIT);
    CsGetPortSchedulesAction firstPageAction =
        new CsGetPortSchedulesAction(
            "subscriberPartyName", "publisherPartyName", supplyAction, null, true);
    CsGetPortSchedulesAction secondPageAction =
        new CsGetPortSchedulesAction(
            "subscriberPartyName", "publisherPartyName", firstPageAction, null, true);

    assertDoesNotThrow(() -> secondPageAction.asJsonNode());
    assertDoesNotThrow(() -> secondPageAction.getHumanReadablePrompt());
    assertDoesNotThrow(() -> secondPageAction.createCheck("1.0.0").subChecksStream().count());
  }
}
