package org.dcsa.conformance.standards.cs.action;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.java.Log;
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
            ? new OverwritingReference<>(null, new DynamicScenarioParameters(null,null, null))
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
    if (dsp.hasCurrentValue()) {
      this.dsp.set(new DynamicScenarioParameters(null,null,null));
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateCursorFromResponsePayload(exchange);
  }

  private void updateCursorFromResponsePayload(ConformanceExchange exchange) {
    DynamicScenarioParameters dspRef = dsp.get();
    Collection<String> linkHeaders = exchange.getResponse().message().headers().get("Link");
    var updatedDsp = dspRef;
    if (linkHeaders != null) {
      Optional<String> link = linkHeaders.stream().findFirst();
      if (link.isPresent()) {
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

    if (!dsp.equals(updatedDsp)) {
      dsp.set(updatedDsp);
    }
  }

  private String getHashString(String actualResponse) {
    String responseHash = "";
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(actualResponse.getBytes());
      responseHash = HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Hashing of the response failed." + e);
    }
    return responseHash;
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
