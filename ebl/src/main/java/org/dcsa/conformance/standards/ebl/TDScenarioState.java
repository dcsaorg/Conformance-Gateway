package org.dcsa.conformance.standards.ebl;

import org.dcsa.conformance.standards.ebl.action.EblAction;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.ebl.sb.ScenarioState;

public interface TDScenarioState<A extends EblAction, S extends ScenarioState<A, S>> extends ScenarioState<A, S> {
  TransportDocumentStatus getCurrentTransportDocumentStatus();
}
