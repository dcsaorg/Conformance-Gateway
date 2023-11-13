package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
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

import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class Carrier extends ConformanceParty {
  private static final Random RANDOM = new Random();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, BookingState> bookingStatesByCbrr = new HashMap<>();
  private final Map<String, String> cbrrToCbr = new HashMap<>();
  protected boolean isShipperNotificationEnabled = false;

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
      targetObjectNode.set(
              "cbrrToCbr",
              StateManagementUtil.storeMap(objectMapper, cbrrToCbr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
        bookingStatesByCbrr, sourceObjectNode.get("bookingStatesByCbrr"), BookingState::valueOf);
      StateManagementUtil.restoreIntoMap(
              cbrrToCbr, sourceObjectNode.get("cbrrToCbr"));
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
    String cbr = cbrrToCbr.get(cbrr);
    BookingState currentState = bookingStatesByCbrr.get(cbrr);
    boolean isCorrect = actionPrompt.get("isCorrect").asBoolean();
    var targetState = BookingState.COMPLETED;
    if (!Objects.equals(BookingState.CONFIRMED, currentState)) {
      throw new IllegalStateException(
              "Booking '%s' is in state '%s'".formatted(cbrr, currentState));
    }
    if (cbr == null && isCorrect) {
        throw new IllegalStateException(
                "Booking '%s' did not have a carrier booking reference".formatted(cbrr));
    }

    if (isCorrect) {
        bookingStatesByCbrr.put(cbrr, targetState);
    }
    var notification = BookingNotification.builder()
              .apiVersion(apiVersion)
              .carrierBookingReference(cbr)
              .bookingStatus(targetState.name())
              .build()
              .asJsonNode();

    if (!isCorrect) {
        ((ObjectNode) notification.get("data")).remove("bookingStatus");
    }

    if (isShipperNotificationEnabled) {
      asyncCounterpartPost("/v1/booking-notifications", notification);
    } else {
      asyncOrchestratorPostPartyInput(
        objectMapper.createObjectNode().put("actionId", actionPrompt.get("actionId").asText()));
    }
    addOperatorLogEntry("Completed the booking request with CBR '%s'".formatted(cbr));
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

  @Builder
  private static class BookingNotification {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    @Builder.ObtainVia(method = "defaultSource")
    private String source;
    private String type;
    private String apiVersion;

    private String carrierBookingReference;
    private String carrierBookingRequestReference;
    private String bookingStatus;

    private String defaultSource() {
            return "https://conformance.dcsa.org";
        }

    private String computedType() {
      if (type != null) {
        return type;
      }
      if (apiVersion != null) {
        var majorVersion = String.valueOf(apiVersion.charAt(0));
        return "org.dcsa.bookingnotification.v" + majorVersion;
      }
      return null;
    }

    public JsonNode asJsonNode() {
      ObjectMapper objectMapper = new ObjectMapper();
      var notification = objectMapper.createObjectNode();
      setIfNotNull(notification, "id", id);
      setIfNotNull(notification, "source", source);
      setIfNotNull(notification, "type", computedType());
      var data = objectMapper.createObjectNode();

      setIfNotNull(notification, "carrierBookingReference", carrierBookingReference);
      setIfNotNull(notification, "carrierBookingRequestReference", carrierBookingRequestReference);
      setIfNotNull(notification, "bookingStatus", bookingStatus);

      if (!data.isEmpty()) {
        notification.putObject("data").put("datacontenttype", "application/json");
      }
      return notification;
    }

    private void setIfNotNull(ObjectNode node, String key, String value) {
      if (value != null) {
        node.put(key, value);
      }
    }
  }
}
