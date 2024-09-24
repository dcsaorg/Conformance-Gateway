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
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.model.InvalidBookingMessageType;

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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    // no state to export
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state to import
  }

  @Override
  protected void doReset() {
    // no state to reset
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(UC1_Shipper_SubmitBookingRequestAction.class, this::sendBookingRequest),
      Map.entry(ShipperGetBookingAction.class, this::getBookingRequest),
      Map.entry(Shipper_GetAmendedBooking404Action.class, this::getBookingRequest),
      Map.entry(UC3_Shipper_SubmitUpdatedBookingRequestAction.class, this::sendUpdatedBooking),
      Map.entry(UC7_Shipper_SubmitBookingAmendment.class, this::sendUpdatedConfirmedBooking),
      Map.entry(UC9_Shipper_CancelBookingAmendment.class, this::sendCancelBookingAmendment),
      Map.entry(UC11_Shipper_CancelBookingRequestAction.class, this::sendCancelBookingRequest),
      Map.entry(UC13ShipperCancelConfirmedBookingAction.class, this::sendConfirmedBookingCancellationRequest),
      Map.entry(AUC_Shipper_SendInvalidBookingAction.class,this::sendInvalidBookingAction));
  }

  private void getBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.getBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.path("cbrr").asText();
    String reference = getBookingReference(actionPrompt);
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("amendedContent", List.of("true"))
      : Collections.emptyMap();

    syncCounterpartGet("/v2/bookings/" + reference, queryParams);

    addOperatorLogEntry("Sent a GET request for booking with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void sendBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    CarrierScenarioParameters carrierScenarioParameters =
        CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));

    JsonNode jsonRequestBody = replaceBookingPlaceHolders(actionPrompt);

    ConformanceResponse conformanceResponse = syncCounterpartPost("/v2/bookings", jsonRequestBody);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String cbrr = jsonBody.path("carrierBookingRequestReference").asText();
    ObjectNode updatedBooking =
      ((ObjectNode) jsonRequestBody)
        .put("carrierBookingRequestReference", cbrr);
    persistentMap.save(cbrr, updatedBooking);

    addOperatorLogEntry(
        "Sent a booking request with the parameters: %s"
            .formatted(carrierScenarioParameters.toJson()));
  }

  private JsonNode replaceBookingPlaceHolders(JsonNode actionPrompt) {

    CarrierScenarioParameters carrierScenarioParameters =
      CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    return JsonToolkit.templateFileToJsonNode(
        "/standards/booking/messages/"+ scenarioType.bookingTemplate(apiVersion),
        Map.ofEntries(
          Map.entry(
            "SERVICE_CONTRACT_REFERENCE_PLACEHOLDER",
            carrierScenarioParameters.serviceContractReference()),
          Map.entry(
            "CONTRACT_QUOTATION_REFERENCE_PLACEHOLDER",
            carrierScenarioParameters.contractQuotationReference()),
          Map.entry(
            "CARRIER_EXPORT_VOYAGE_NUMBER_PLACEHOLDER", carrierScenarioParameters.carrierExportVoyageNumber()),
          Map.entry(
            "CARRIER_SERVICE_NAME_PLACEHOLDER", carrierScenarioParameters.carrierServiceName()),
          Map.entry(
            "COMMODITY_HS_CODE_1", carrierScenarioParameters.hsCodes1()),
          Map.entry(
            "COMMODITY_HS_CODE_2", carrierScenarioParameters.hsCodes1()),
          Map.entry(
            "COMMODITY_TYPE_1_PLACEHOLDER", carrierScenarioParameters.commodityType1() ),
          Map.entry(
            "COMMODITY_TYPE_2_PLACEHOLDER", carrierScenarioParameters.commodityType2() ),
          Map.entry(
            "POL_UNLOCATION_CODE_PLACEHOLDER", carrierScenarioParameters.polUNLocationCode()),
          Map.entry(
            "POD_UNLOCATION_CODE_PLACEHOLDER", carrierScenarioParameters.podUNLocationCode()) ));
  }

  private void sendCancelBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.path("cbrr").asText();
    syncCounterpartPatch(
        "/v2/bookings/%s".formatted(cbrr),
      Collections.emptyMap(),
        new ObjectMapper()
            .createObjectNode()
            .put("bookingStatus", BookingState.CANCELLED.name()));

    addOperatorLogEntry("Sent a cancel booking request of '%s'".formatted(cbrr));
  }

  private void sendConfirmedBookingCancellationRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendConfirmedBookingCancellationRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbr = actionPrompt.path("cbr").asText();
    syncCounterpartPatch(
      "/v2/bookings/%s".formatted(cbr),
      Collections.emptyMap(),
      new ObjectMapper()
        .createObjectNode()
        .put("bookingCancellationStatus", BookingCancellationState.CANCELLATION_RECEIVED.name())
        .put("reason", "Cancelling due to internal issues"));

    addOperatorLogEntry("Sent a confirmed booking cancellation of '%s'".formatted(cbr));
  }

  private void sendCancelBookingAmendment(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));
    String reference = getBookingReference(actionPrompt);
    syncCounterpartPatch(
      "/v2/bookings/%s".formatted(reference),
      Collections.emptyMap(),
      new ObjectMapper()
        .createObjectNode()
        .put("amendedBookingStatus", BookingState.AMENDMENT_CANCELLED.name()));

    addOperatorLogEntry("Sent a cancel amendment request of '%s'".formatted(reference));
  }

  private void sendInvalidBookingAction(JsonNode actionPrompt) {
    log.info("Shipper.sendInvalidBookingAction(%s)".formatted(actionPrompt.toPrettyString()));
    var invalidBookingMessageType = InvalidBookingMessageType.valueOf(actionPrompt.required("invalidBookingMessageType").asText("<?>"));
    var cbrr = actionPrompt.required("cbrr").asText();
    switch (invalidBookingMessageType) {
      case UPDATE_BOOKING -> sendUpdatedBooking(actionPrompt);
      case SUBMIT_BOOKING_AMENDMENT  -> sendUpdatedConfirmedBooking(actionPrompt);
      case CANCEL_BOOKING_AMENDMENT  -> sendCancelBookingAmendment(actionPrompt);
      case CANCEL_BOOKING  -> sendCancelBookingRequest(actionPrompt);
      default -> throw new AssertionError("Missing case for " + invalidBookingMessageType.name());
    }
    addOperatorLogEntry("Sent a invalid booking action request of '%s'".formatted(cbrr));
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
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(new ObjectMapper().createObjectNode()));

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
