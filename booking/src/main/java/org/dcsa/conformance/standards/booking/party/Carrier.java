package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;

@Slf4j
public class Carrier extends ConformanceParty {
  private final Map<String, BookingState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> sirsByTdr = new HashMap<>();
  private final Map<String, String> brsByTdr = new HashMap<>();

  public Carrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        asyncWebClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    ObjectMapper objectMapper = new ObjectMapper();
    {
      ArrayNode arrayNode = objectMapper.createArrayNode();
      eblStatesByTdr.forEach(
          (key, value) -> {
            ObjectNode entryNode = objectMapper.createObjectNode();
            entryNode.put("key", key);
            entryNode.put("value", value.name());
            arrayNode.add(entryNode);
          });
      targetObjectNode.set("eblStatesByTdr", arrayNode);
    }
    {
      ArrayNode arrayNode = objectMapper.createArrayNode();
      sirsByTdr.forEach(
          (key, value) -> {
            ObjectNode entryNode = objectMapper.createObjectNode();
            entryNode.put("key", key);
            entryNode.put("value", value);
            arrayNode.add(entryNode);
          });
      targetObjectNode.set("sirsByTdr", arrayNode);
    }
    {
      ArrayNode arrayNode = objectMapper.createArrayNode();
      brsByTdr.forEach(
          (key, value) -> {
            ObjectNode entryNode = objectMapper.createObjectNode();
            entryNode.put("key", key);
            entryNode.put("value", value);
            arrayNode.add(entryNode);
          });
      targetObjectNode.set("brsByTdr", arrayNode);
    }
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesByTdr.put(
                    entryNode.get("key").asText(),
                    BookingState.valueOf(entryNode.get("value").asText())));
    StreamSupport.stream(sourceObjectNode.get("sirsByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                sirsByTdr.put(entryNode.get("key").asText(), entryNode.get("value").asText()));
    StreamSupport.stream(sourceObjectNode.get("brsByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                brsByTdr.put(entryNode.get("key").asText(), entryNode.get("value").asText()));
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
    sirsByTdr.clear();
    brsByTdr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(UC1_Shipper_SubmitBookingRequestAction.class, this::sendBookingRequest));
  }

  private void sendBookingRequest(JsonNode actionPrompt) {
    log.info("BookingCarrier.sendBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr =
        actionPrompt.has("tdr")
            ? actionPrompt.get("tdr").asText()
            : UUID.randomUUID().toString().substring(20);
    String sir = sirsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString());
    String br =
        brsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString().substring(35));

    boolean isCorrect = actionPrompt.get("isCorrect").asBoolean();
    if (isCorrect) {
      eblStatesByTdr.put(tdr, BookingState.RECEIVED);
    }

    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/booking/messages/booking-%s-request.json"
                .formatted(apiVersion.startsWith("3") ? "v30" : "v20"),
            Map.ofEntries(
                Map.entry("TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", tdr),
                Map.entry("SHIPPING_INSTRUCTION_REFERENCE_PLACEHOLDER", sir),
                Map.entry("BOOKING_REFERENCE_PLACEHOLDER", br)));
    if (!isCorrect) {
      ((ObjectNode) jsonRequestBody.get("document")).remove("issuingParty");
    }

    asyncCounterpartPost("/v1/issue-ebls", jsonRequestBody);

    addOperatorLogEntry(
        "Sent a %s booking request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(isCorrect ? "correct" : "incorrect", tdr, eblStatesByTdr.get(tdr)));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("BookingCarrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String tdr = jsonRequest.get("transportDocumentReference").asText();
    String irc = jsonRequest.get("bookingResponseCode").asText();

    if (Objects.equals(BookingState.RECEIVED, eblStatesByTdr.get(tdr))) {

      addOperatorLogEntry(
          "Handling booking response with bookingResponseCode '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
              .formatted(irc, tdr, eblStatesByTdr.get(tdr)));

      return request.createResponse(
          204,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(new ObjectMapper().createObjectNode()));
    } else {
      return request.createResponse(
          409,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(
              new ObjectMapper()
                  .createObjectNode()
                  .put(
                      "message",
                      "Rejecting '%s' for eBL '%s' because it is in state '%s'"
                          .formatted(irc, tdr, eblStatesByTdr.get(tdr)))));
    }
  }
}
