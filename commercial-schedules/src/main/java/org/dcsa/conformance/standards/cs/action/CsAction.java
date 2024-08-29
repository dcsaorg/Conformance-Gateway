package org.dcsa.conformance.standards.cs.action;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

public abstract class CsAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  public CsAction(
      String sourcePartyName,
      String targetPartyName,
      CsAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
    this.dsp =
        previousAction == null
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(null))
            : new OverwritingReference<>(previousAction.dsp, null);
  }

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction
            instanceof SupplyScenarioParametersAction supplyScenarioParametersActionAction
        ? supplyScenarioParametersActionAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateCursorFromResponsePayload(exchange);
  }

  private void updateCursorFromResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dspRef = dsp.get();
    Collection<String> linkHeaders = exchange.getResponse().message().headers().get("Link");
    Optional<String> link = linkHeaders.stream().findFirst();
    var updatedDsp = dspRef;
    if(link.isPresent()){
      String[] links = link.get().split(",");
      for (String value : links) {
        String url = value.split(";")[0];
        String rel = value.split(";")[1];
        String relValue = rel.split("=")[1].replace("\"", "");
        if ("next".equals(relValue)) {
          String cursorValue = extractCursorValue(url);
          updatedDsp = updateIfNotNull(updatedDsp, cursorValue, updatedDsp::withCursor);
        }
      }
    }

  }

  private static String extractCursorValue(String url) {
    String[] urlParts = url.split("\\?");
    if (urlParts.length > 1) {
      String query = urlParts[1];
      String[] parameters = query.split("&");
      for (String param : parameters) {
        String[] keyValue = param.split("=");
        if (keyValue.length == 2 && "cursor".equals(keyValue[0])) {
          return keyValue[1];
        }
      }
    }
    return null;
  }
  private <T> DynamicScenarioParameters updateIfNotNull(DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
    if (value == null) {
      return dsp;
    }
    return with.apply(value);
  }
  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp.hasCurrentValue()) {
      jsonState.set("currentDsp", dsp.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      dsp.set(DynamicScenarioParameters.fromJson(dspNode));
    }
  }
}
