package org.dcsa.conformance.standards.ovs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ovs.party.SuppliedScenarioParameters;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public abstract class OvsAction extends ConformanceAction {
  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;
  private String paginationCursor;

  public OvsAction(
    String sourcePartyName,
    String targetPartyName,
    ConformanceAction previousAction,
    String actionTitle,
    int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
    this.paginationCursor = previousAction instanceof OvsAction ? ((OvsAction) previousAction).paginationCursor : null;
  }

  @Override
  public void reset() {
    super.reset();
    paginationCursor = null;
  }

  protected Supplier<String> getPaginationCursorSupplier() {
    return () -> paginationCursor;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    Collection<String> paginationHeaders =
      exchange.getResponse().message().headers().get("Next-Page-Cursor");
    if (paginationHeaders != null) {
      paginationHeaders.stream().findFirst().ifPresent(cursor -> paginationCursor = cursor);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (paginationCursor != null) {
      jsonState.put("paginationCursor", paginationCursor);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has("paginationCursor")) {
      paginationCursor = jsonState.get("paginationCursor").asText();
    }
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }
}
