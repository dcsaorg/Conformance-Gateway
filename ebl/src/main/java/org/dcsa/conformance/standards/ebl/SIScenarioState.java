package org.dcsa.conformance.standards.ebl;

import org.dcsa.conformance.standards.ebl.action.EblAction;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.sb.ScenarioState;

public interface SIScenarioState<A extends EblAction, S extends ScenarioState<A, S>> extends ScenarioState<A, S> {
  ShippingInstructionsStatus getShippingInstructionsStatus();
  ShippingInstructionsStatus getUpdatedShippingInstructionsStatus();
}
