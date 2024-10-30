package org.dcsa.conformance.standards.tnt.action;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.tnt.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;

@Slf4j
public abstract class TntAction extends ConformanceAction {
  protected final Supplier<SuppliedScenarioParameters> sspSupplier;
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dsp;

  public TntAction(
      String sourcePartyName,
      String targetPartyName,
      TntAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.sspSupplier = _getSspSupplier(previousAction);
    this.expectedStatus = expectedStatus;
    this.dsp = previousAction == null
      ? new OverwritingReference<>(null,
      new DynamicScenarioParameters(null, null, null,null, null))
      : new OverwritingReference<>(previousAction.dsp, null);
  }

  protected TntAction getPreviousTntAction() {
    return (TntAction) previousAction;
  }


  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    } else {
      this.dsp.set(new DynamicScenarioParameters(null, null, null,null, null));
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
        updatedDsp = updateIfNotNull(updatedDsp, secondPageHash, updatedDsp::withLastPage);
      }
    }

    if (!dsp.equals(updatedDsp)) dsp.set(updatedDsp);
  }

  private <T> DynamicScenarioParameters updateIfNotNull(
    DynamicScenarioParameters dsp, T value, Function<T, DynamicScenarioParameters> with) {
    if (value == null) {
      return dsp;
    }
    return with.apply(value);
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

  private Supplier<SuppliedScenarioParameters> _getSspSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SupplyScenarioParametersAction supplyAvailableTdrAction
        ? supplyAvailableTdrAction::getSuppliedScenarioParameters
        : previousAction == null
            ? () -> SuppliedScenarioParameters.fromMap(Map.ofEntries())
            : _getSspSupplier(previousAction.getPreviousAction());
  }
}
