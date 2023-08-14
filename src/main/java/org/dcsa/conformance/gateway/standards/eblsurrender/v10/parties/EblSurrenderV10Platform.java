package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10State;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.RequestSurrenderAction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EblSurrenderV10Platform extends ConformanceParty {
  private final Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();

  public EblSurrenderV10Platform(
      String name, boolean internal, String gatewayBaseUrl, String gatewayRootPath) {
    super(name, internal, gatewayBaseUrl, gatewayRootPath);
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(RequestSurrenderAction.class, this::requestSurrender));
  }

  private void requestSurrender(JsonNode actionPrompt) {
    String tdr = actionPrompt.get("tdr").asText();
    boolean forAmendment = actionPrompt.get("forAmendment").booleanValue();
    eblStatesById.put(
        tdr,
        forAmendment
            ? EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED
            : EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED);
    postAsync(
        gatewayRootPath + "/v1/surrender-requests",
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", UUID.randomUUID().toString())
            .put("transportDocumentReference", tdr)
            .put("surrenderRequestCode", forAmendment ? "AREQ" : "SREQ"));
  }

  @Override
  public ResponseEntity<JsonNode> handlePostRequest(JsonNode requestBody) {
    String action = requestBody.get("action").asText();
    String tdr = requestBody.get("transportDocumentReference").asText();
    if (Objects.equals(EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("SURR", action)
              ? EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      return new ResponseEntity<>(
          new ObjectMapper().createObjectNode(),
          HttpStatus.NO_CONTENT);
    } else if (Objects.equals(EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
              tdr,
              Objects.equals("SURR", action)
                      ? EblSurrenderV10State.SURRENDERED_FOR_DELIVERY
                      : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      return new ResponseEntity<>(
              new ObjectMapper().createObjectNode(),
              HttpStatus.NO_CONTENT);
    } else {
      return new ResponseEntity<>(
          new ObjectMapper()
              .createObjectNode()
              .put(
                  "comments",
                  "Rejecting '%s' for document '%s' because it is in state '%s'"
                      .formatted(action, tdr, eblStatesById.get(tdr))),
          HttpStatus.CONFLICT);
    }
  }
}
