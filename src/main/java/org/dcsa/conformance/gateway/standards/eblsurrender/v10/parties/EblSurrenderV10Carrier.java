package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10State;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.AcceptSurrenderRequestAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.AmendDocumentOfflineAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.RejectSurrenderRequestAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EblSurrenderV10Carrier extends ConformanceParty {
  private final Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();

  public EblSurrenderV10Carrier(
      String name, boolean internal, String gatewayBaseUrl, String gatewayRootPath) {
    super(name, internal, gatewayBaseUrl, gatewayRootPath);
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyAvailableTdrAction.class, this::supplyAvailableTdr),
        Map.entry(AcceptSurrenderRequestAction.class, this::acceptSurrenderRequest),
        Map.entry(RejectSurrenderRequestAction.class, this::rejectSurrenderRequest),
        Map.entry(AmendDocumentOfflineAction.class, this::amendDocumentOffline));
  }

  private void supplyAvailableTdr(JsonNode actionPrompt) {
    String tdr = UUID.randomUUID().toString();
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    postAsync(
        "/party/input",
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .put("tdr", tdr));
  }

  private void acceptSurrenderRequest(JsonNode actionPrompt) {
    String tdr = actionPrompt.get("tdr").asText();
    switch (eblStatesById.get(tdr)) {
      case AMENDMENT_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr, EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT);
      case DELIVERY_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr, EblSurrenderV10State.SURRENDERED_FOR_DELIVERY);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
    postAsync(
        gatewayRootPath + "/v1/surrender-request-responses",
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", actionPrompt.get("srr").asText())
            .put("action", "SURR"));
  }

  private void rejectSurrenderRequest(JsonNode actionPrompt) {
    String tdr = actionPrompt.get("tdr").asText();
    switch (eblStatesById.get(tdr)) {
      case AMENDMENT_SURRENDER_REQUESTED, DELIVERY_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
    postAsync(
        gatewayRootPath + "/v1/surrender-request-responses",
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", actionPrompt.get("srr").asText())
            .put("action", "SREJ"));
  }

  private void amendDocumentOffline(JsonNode actionPrompt) {
    String tdr = actionPrompt.get("tdr").asText();
    switch (eblStatesById.get(tdr)) {
      case SURRENDERED_FOR_AMENDMENT -> eblStatesById.put(
          tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
  }

  @Override
  public ResponseEntity<JsonNode> handlePostRequest(JsonNode requestBody) {
    String srr = requestBody.get("surrenderRequestReference").asText();
    String tdr = requestBody.get("transportDocumentReference").asText();
    String src = requestBody.get("surrenderRequestCode").asText();
    if (Objects.equals(EblSurrenderV10State.AVAILABLE_FOR_SURRENDER, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("AREQ", src)
              ? EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED
              : EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED);
      return new ResponseEntity<>(
          new ObjectMapper()
              .createObjectNode()
              .put("surrenderRequestReference", srr)
              .put("transportDocumentReference", tdr),
          HttpStatus.ACCEPTED);
    } else {
      return new ResponseEntity<>(
          new ObjectMapper()
              .createObjectNode()
              .put(
                  "comments",
                  "Rejecting '%s' for document '%s' because it is in state '%s'"
                      .formatted(src, tdr, eblStatesById.get(tdr))),
          HttpStatus.CONFLICT);
    }
  }
}
