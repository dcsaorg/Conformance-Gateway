package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.*;

@Slf4j
public class BookingShipper extends ConformanceParty {

  private static final String SERVICE_CONTRACT_REF = "serviceContractReference";
  private static final String SERVICE_REF_PUT = "serviceRefPut";
  public BookingShipper(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
  }

  @Override
  public void exportPartyJsonState(ObjectNode targetObjectNode) {
    // no state to export
  }

  @Override
  public void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state to import
  }

  @Override
  public void doReset() {
    // no state to reset
  }

  @Override
  public Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(UC1_Shipper_SubmitBookingRequestAction.class, this::sendBookingRequest),
        Map.entry(ShipperGetBookingAction.class, this::getBookingRequest),
        Map.entry(UC3_Shipper_SubmitUpdatedBookingRequestAction.class, this::sendUpdatedBooking),
        Map.entry(UC7_Shipper_SubmitBookingAmendment.class, this::sendUpdatedConfirmedBooking),
        Map.entry(UC9_Shipper_CancelBookingAmendment.class, this::sendCancelBookingAmendment),
        Map.entry(UC11_Shipper_CancelBookingRequestAction.class, this::sendCancelBookingRequest),
        Map.entry(
            UC13ShipperCancelConfirmedBookingAction.class,
            this::sendConfirmedBookingCancellationRequest),
        Map.entry(ShipperGetBookingErrorScenarioAction.class, this::getBookingRequest));
  }

  private void getBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.getBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.path("cbrr").asText();
    String reference = getBookingReference(actionPrompt);
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    boolean errorScenario = actionPrompt.path("invalidBookingReference").asBoolean(false);
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("amendedContent", List.of("true"))
      : Collections.emptyMap();
    if (errorScenario) {
      syncCounterpartGet("/v2/bookings/" + "ABC123", queryParams);
      cbrr = "ABC123";
    } else {
      syncCounterpartGet("/v2/bookings/" + reference, queryParams);
    }

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Sent a GET request for booking", cbr, cbrr));
  }

  private void sendBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    JsonNode bookingPayload = actionPrompt.get("bookingPayload");

    ConformanceResponse conformanceResponse = syncCounterpartPost("/v2/bookings", bookingPayload);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String cbrr = jsonBody.path("carrierBookingRequestReference").asText();
    ObjectNode updatedBooking =
        ((ObjectNode) bookingPayload).put("carrierBookingRequestReference", cbrr);
    persistentMap.save(cbrr, updatedBooking);

    addOperatorLogEntry("Sent a booking request with the parameters: %s".formatted(bookingPayload));
  }

  private void sendCancelBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.path("cbrr").asText();
    syncCounterpartPatch(
        "/v2/bookings/%s".formatted(cbrr),
      Collections.emptyMap(),
      OBJECT_MAPPER
            .createObjectNode()
            .put("bookingStatus", BookingState.CANCELLED.name()));

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt(
            "Sent a cancel booking request to cancel booking", null, cbrr));
  }

  private void sendConfirmedBookingCancellationRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendConfirmedBookingCancellationRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbr = actionPrompt.path("cbr").asText();
    syncCounterpartPatch(
      "/v2/bookings/%s".formatted(cbr),
      Collections.emptyMap(),
      OBJECT_MAPPER
        .createObjectNode()
        .put("bookingCancellationStatus", BookingCancellationState.CANCELLATION_RECEIVED.name())
        .put("reason", "Cancelling due to internal issues"));

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt(
            "Sent a confirmed booking cancellation of booking", cbr, null));
  }

  private void sendCancelBookingAmendment(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));
    String reference = getBookingReference(actionPrompt);
    syncCounterpartPatch(
      "/v2/bookings/%s".formatted(reference),
      Collections.emptyMap(),
      OBJECT_MAPPER
        .createObjectNode()
        .put("amendedBookingStatus", BookingState.AMENDMENT_CANCELLED.name()));

    addOperatorLogEntry("Sent a cancel amendment request of '%s'".formatted(reference));
  }

  private void sendUpdatedBooking(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedBooking(%s)".formatted(actionPrompt.toPrettyString()));
    String reference = getBookingReference(actionPrompt);
    String cbrr = actionPrompt.path("cbrr").asText();
    var bookingData = persistentMap.load(cbrr);
    ((ObjectNode) bookingData).put(SERVICE_CONTRACT_REF, SERVICE_REF_PUT);
    syncCounterpartPut(
      "/v2/bookings/%s".formatted(reference),bookingData);

    addOperatorLogEntry(
      "Sent an updated booking request with the parameters: %s"
        .formatted(reference));
  }

  private void sendUpdatedConfirmedBooking(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));
    String reference = getBookingReference(actionPrompt);
    String cbrr = actionPrompt.path("cbrr").asText();
    var bookingData = persistentMap.load(cbrr);
    ((ObjectNode) bookingData).put(SERVICE_CONTRACT_REF, SERVICE_REF_PUT);
    syncCounterpartPut(
      "/v2/bookings/%s".formatted(reference),bookingData);

    addOperatorLogEntry(
      "Sent an updated confirmed booking with the parameters: %s"
        .formatted(reference));
  }


  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Shipper.handleRequest(%s)".formatted(request));
    ConformanceResponse response =
        request.createResponse(
            204,
            Map.of(API_VERSION, List.of(apiVersion)),
            new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));

    addOperatorLogEntry(
        "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }

  private String getBookingReference(JsonNode actionPrompt ) {
    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.path("cbrr").asText();
    return  cbr != null ? cbr : cbrr;
  }

}
