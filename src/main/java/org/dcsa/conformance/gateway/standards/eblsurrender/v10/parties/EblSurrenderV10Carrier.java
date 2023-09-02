package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.configuration.CounterpartConfiguration;
import org.dcsa.conformance.gateway.configuration.PartyConfiguration;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10State;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SurrenderResponseAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.VoidAndReissueAction;
import org.dcsa.conformance.gateway.traffic.ConformanceMessageBody;
import org.dcsa.conformance.gateway.traffic.ConformanceRequest;
import org.dcsa.conformance.gateway.traffic.ConformanceResponse;

@Slf4j
public class EblSurrenderV10Carrier extends ConformanceParty {
  private final Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();

  public EblSurrenderV10Carrier(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient) {
    super(partyConfiguration, counterpartConfiguration, asyncWebClient);
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyAvailableTdrAction.class, this::supplyAvailableTdr),
        Map.entry(SurrenderResponseAction.class, this::sendSurrenderResponse),
        Map.entry(VoidAndReissueAction.class, this::voidAndReissue));
  }

  private synchronized void supplyAvailableTdr(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderV10Carrier.supplyAvailableTdr(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    asyncOrchestratorPostPartyInput(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .put("tdr", tdr));
  }

  private synchronized void voidAndReissue(JsonNode actionPrompt) {
    log.info("EblSurrenderV10Carrier.voidAndReissue(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.get("tdr").asText();
    if (!Objects.equals(eblStatesById.get(tdr), EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT)) {
      throw new IllegalStateException(
          "Cannot void and reissue in state: " + eblStatesById.get(tdr));
    }
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    asyncOrchestratorPostPartyInput(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .put("tdr", tdr));
  }

  private synchronized void sendSurrenderResponse(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderV10Carrier.sendSurrenderResponse(%s)"
            .formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.get("tdr").asText();
    boolean accept = actionPrompt.get("accept").asBoolean();
    switch (eblStatesById.get(tdr)) {
      case AMENDMENT_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      case DELIVERY_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderV10State.SURRENDERED_FOR_DELIVERY
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
    String srr = actionPrompt.get("srr").asText();
    if ("*".equals(srr)) {
      srr = UUID.randomUUID().toString();
    }
    asyncCounterpartPost(
        "/v1/surrender-request-responses",
        "1.0.0",
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("action", accept ? "SURR" : "SREJ"),
        conformanceResponse -> {});
  }

  @Override
  public synchronized ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderV10Carrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = jsonRequest.get("transportDocumentReference").asText();
    String src = jsonRequest.get("surrenderRequestCode").asText();
    if (Objects.equals(EblSurrenderV10State.AVAILABLE_FOR_SURRENDER, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("AREQ", src)
              ? EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED
              : EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED);
      return request.createResponse(
          202,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(
              new ObjectMapper()
                  .createObjectNode()
                  .put("surrenderRequestReference", srr)
                  .put("transportDocumentReference", tdr)));
    } else {
      return request.createResponse(
          409,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(
              new ObjectMapper()
                  .createObjectNode()
                  .put(
                      "comments",
                      "Rejecting '%s' for document '%s' because it is in state '%s'"
                          .formatted(src, tdr, eblStatesById.get(tdr)))));
    }
  }
}
