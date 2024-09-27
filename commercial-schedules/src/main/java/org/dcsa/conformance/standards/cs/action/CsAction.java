package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.cs.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

@Slf4j
public abstract class CsAction extends ConformanceAction {

  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected CsAction(
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
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(null, null, null))
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
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null, null, null));
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateCursorFromResponsePayload(exchange);
  }

  private void updateCursorFromResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dspRef = dsp.get();
    Collection<String> paginationHeaders =
        exchange.getResponse().message().headers().get("Next-Page-Cursor");
    var updatedDsp = dspRef;
    if (paginationHeaders != null) {
      Optional<String> cursor = paginationHeaders.stream().findFirst();
      if (cursor.isPresent()) {
        updatedDsp = updateIfNotNull(updatedDsp, cursor.get(), updatedDsp::withCursor);
      }
    }
    String jsonResponse = exchange.getResponse().message().body().toString();
    if (previousAction != null) {
      if (previousAction instanceof SupplyScenarioParametersAction) {
        String firstPageHash = getHashString(jsonResponse);
        updatedDsp = updateIfNotNull(updatedDsp, firstPageHash, updatedDsp::withFirstPage);
      } else {
        String secondPageHash = getHashString(jsonResponse);
        updatedDsp = updateIfNotNull(updatedDsp, secondPageHash, updatedDsp::withSecondPage);
      }
    }

    if (!dsp.equals(updatedDsp)) dsp.set(updatedDsp);
  }

  private String getHashString(String actualResponse) {
    String responseHash = "";
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(actualResponse.getBytes());
      responseHash = HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Hashing of the response failed.", e);
    }
    return responseHash;
  }

  private <T> DynamicScenarioParameters updateIfNotNull(
      DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
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
