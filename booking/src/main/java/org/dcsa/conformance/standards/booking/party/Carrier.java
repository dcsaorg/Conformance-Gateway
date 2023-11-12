package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.Carrier_SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.booking.action.UC11_Carrier_ConfirmBookingCompletedAction;
import org.dcsa.conformance.standards.booking.action.UC5_Carrier_ConfirmBookingRequestAction;

@Slf4j
public class Carrier extends ConformanceParty {
  private static final Random RANDOM = new Random();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, BookingState> bookingStatesByCbrr = new HashMap<>();

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
    targetObjectNode.set(
        "bookingStatesByCbrr",
        StateManagementUtil.storeMap(objectMapper, bookingStatesByCbrr, BookingState::name));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
        bookingStatesByCbrr, sourceObjectNode.get("bookingStatesByCbrr"), BookingState::valueOf);
  }

  @Override
  protected void doReset() {
    bookingStatesByCbrr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(Carrier_SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(UC5_Carrier_ConfirmBookingRequestAction.class, this::confirmBookingRequest),
        Map.entry(UC11_Carrier_ConfirmBookingCompletedAction.class, this::confirmBookingCompleted));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("Carrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    CarrierScenarioParameters carrierScenarioParameters =
        new CarrierScenarioParameters(
            "Carrier Service %d".formatted(RANDOM.nextInt(999999)),
            "9%06d".formatted(RANDOM.nextInt(999999)));
    asyncOrchestratorPostPartyInput(
        objectMapper
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
        "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  private void confirmBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    BookingState currentState = bookingStatesByCbrr.get(cbrr);
    if (!Set.of(BookingState.RECEIVED, BookingState.PENDING_UPDATE_CONFIRMATION)
        .contains(currentState)) {
      throw new IllegalStateException(
          "Booking '%s' is in state '%s'".formatted(cbrr, currentState));
    }

    bookingStatesByCbrr.put(cbrr, BookingState.CONFIRMED);

    asyncOrchestratorPostPartyInput(
        objectMapper.createObjectNode().put("actionId", actionPrompt.get("actionId").asText()));

    addOperatorLogEntry("Confirmed the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void confirmBookingCompleted(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingCompleted(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    BookingState currentState = bookingStatesByCbrr.get(cbrr);
    if (!Objects.equals(BookingState.CONFIRMED, currentState)) {
      throw new IllegalStateException(
              "Booking '%s' is in state '%s'".formatted(cbrr, currentState));
    }

    bookingStatesByCbrr.put(cbrr, BookingState.COMPLETED);

    asyncOrchestratorPostPartyInput(
            objectMapper.createObjectNode().put("actionId", actionPrompt.get("actionId").asText()));

    addOperatorLogEntry("Completed the booking request with CBRR '%s'".formatted(cbrr));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Carrier.handleRequest(%s)".formatted(request));
    if (request.method().equals("GET")) {
      return _handleGetBookingRequest(request);
    } else if (request.method().equals("POST") && request.url().endsWith("/v2/bookings")) {
      return _handlePostBookingRequest(request);
    } else {
      ConformanceResponse response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  objectMapper
                      .createObjectNode()
                      .put(
                          "message",
                          "Rejecting '%s' for Booking '%s' because it is in state '%s'"
                              .formatted("TODO", "TODO", "TODO"))));
      addOperatorLogEntry(
          "Handling booking request '%s' (now in state '%s')".formatted("TODO", "TODO"));
      return response;
    }
  }

  private ConformanceResponse _handleGetBookingRequest(ConformanceRequest request) {
    String cbrr = request.url().substring(1 + request.url().lastIndexOf("/"));
    ConformanceResponse response =
        request.createResponse(
            200,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(
                objectMapper
                    .createObjectNode()
                    .put("carrierBookingRequestReference", cbrr)
                    .put("bookingStatus", bookingStatesByCbrr.get(cbrr).name())
                    .put("TODO", "...")));
    addOperatorLogEntry(
        "Responded to GET booking request '%s' (in state '%s')"
            .formatted(cbrr, BookingState.RECEIVED.name()));
    return response;
  }

  private ConformanceResponse _handlePostBookingRequest(ConformanceRequest request) {
    String cbrr = UUID.randomUUID().toString();
    BookingState bookingState = BookingState.RECEIVED;
    bookingStatesByCbrr.put(cbrr, bookingState);
    ConformanceResponse response =
        request.createResponse(
            201,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(
                objectMapper
                    .createObjectNode()
                    .put("carrierBookingRequestReference", cbrr)
                    .put("bookingStatus", bookingState.name())));
    addOperatorLogEntry(
        "Accepted booking request '%s' (now in state '%s')".formatted(cbrr, bookingState.name()));
    return response;
  }
}
