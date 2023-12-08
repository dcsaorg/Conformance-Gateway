package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.*;

@Slf4j
public class Shipper extends ConformanceParty {

  private static final String SERVICE_CONTRACT_REF = "serviceContractReference";
  private static final String SERVICE_REF_PUT = "serviceRefPut";
  private static final String FILE_SUFFIX_REEFER = "-reefer";
  public Shipper(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
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
        Map.entry(Shipper_GetBookingAction.class, this::getBookingRequest),
        Map.entry(Shipper_GetAmendedBooking404Action.class, this::getBookingRequest),
        Map.entry(UC3_Shipper_SubmitUpdatedBookingRequestAction.class, this::sendUpdatedBooking),
        Map.entry(UC7_Shipper_SubmitBookingAmendment.class, this::sendUpdatedConfirmedBooking),
        Map.entry(UC9_Shipper_CancelBookingAmendment.class, this::sendCancelBookingAmendment),
        Map.entry(UC12_Shipper_CancelEntireBookingAction.class, this::sendCancelEntireBooking));
  }

  private void getBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.getBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("amendedContent", List.of("true"))
      : Collections.emptyMap();

    asyncCounterpartGet("/v2/bookings/" + cbrr, queryParams);

    addOperatorLogEntry("Sent a GET request for booking with CBRR: %s".formatted(cbrr));
  }

  private void sendBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    CarrierScenarioParameters carrierScenarioParameters =
        CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));

    JsonNode jsonRequestBody = replaceBookingPlaceHolders(actionPrompt);

    asyncCounterpartPost(
        "/v2/bookings",
        jsonRequestBody,
        conformanceResponse -> {
          JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
          String cbrr = jsonBody.path("carrierBookingRequestReference").asText();
          String bookingStatus = jsonBody.path("bookingStatus").asText();
          ObjectNode updatedBooking =
                ((ObjectNode) jsonRequestBody)
                  .put("bookingStatus", bookingStatus)
                  .put("carrierBookingRequestReference", cbrr);
          persistentMap.save(cbrr, updatedBooking);
        });

    addOperatorLogEntry(
        "Sent a booking request with the parameters: %s"
            .formatted(carrierScenarioParameters.toJson()));
  }

  private JsonNode replaceBookingPlaceHolders(JsonNode actionPrompt) {

    CarrierScenarioParameters carrierScenarioParameters =
      CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));
    String bookingVariant = actionPrompt.get("bookingVariant").asText();

    String fileSuffix = bookingVariant.equals("reefer") ? FILE_SUFFIX_REEFER: "";
    return JsonToolkit.templateFileToJsonNode(
        "/standards/booking/messages/booking-api-v20%s-request.json".formatted(fileSuffix),
        Map.ofEntries(
          Map.entry(
            "CONTRACT_QUOTATION_REFERENCE_PLACEHOLDER",
            carrierScenarioParameters.contractQuotationReference()),
          Map.entry(
            "CARRIER_EXPORT_VOYAGE_NUMBER_PLACEHOLDER", carrierScenarioParameters.carrierExportVoyageNumber()),
          Map.entry(
            "CARRIER_SERVICE_NAME_PLACEHOLDER", carrierScenarioParameters.carrierServiceName()),
          Map.entry(
            "COMMODITY_HS_CODE", carrierScenarioParameters.hsCodes()),
          Map.entry(
            "COMMODITY_TYPE_PLACEHOLDER", carrierScenarioParameters.commodityType() ),
          Map.entry(
            "POL_UNLOCATION_CODE_PLACEHOLDER", carrierScenarioParameters.polUNLocationCode()),
          Map.entry(
            "POD_UNLOCATION_CODE_PLACEHOLDER", carrierScenarioParameters.podUNLocationCode()) ));
  }

  private void sendCancelEntireBooking(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelEntireBooking(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();
    Map<String, List<String>> queryParams =  Map.of("operation", List.of("cancelBooking"));
    asyncCounterpartPatch(
        "/v2/bookings/%s".formatted(cbrr),
        queryParams,
        new ObjectMapper()
            .createObjectNode()
            .put("bookingStatus", BookingState.CANCELLED.wireName()));

    addOperatorLogEntry("Sent a cancel booking request of '%s'".formatted(cbrr));
  }

  private void sendCancelBookingAmendment(JsonNode actionPrompt) {
    log.info("Shipper.sendCancelBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();
    Map<String, List<String>> queryParams =  Map.of("operation", List.of("cancelAmendment"));
    asyncCounterpartPatch(
      "/v2/bookings/%s".formatted(cbrr),
      queryParams,
      new ObjectMapper()
        .createObjectNode()
        .put("bookingStatus", BookingState.CANCELLED.wireName()));

    addOperatorLogEntry("Sent a cancel amendment request of '%s'".formatted(cbrr));
  }

  private void sendUpdatedBooking(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedBooking(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();

    var bookingData = persistentMap.load(cbrr);
    ((ObjectNode) bookingData).put(SERVICE_CONTRACT_REF, SERVICE_REF_PUT);
    asyncCounterpartPut(
      "/v2/bookings/%s".formatted(cbrr),bookingData);

    addOperatorLogEntry(
      "Sent an updated booking request with the parameters: %s"
        .formatted(cbrr));
  }

  private void sendUpdatedConfirmedBooking(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();

    var bookingData = persistentMap.load(cbrr);
    ((ObjectNode) bookingData).put(SERVICE_CONTRACT_REF, SERVICE_REF_PUT);
    asyncCounterpartPut(
      "/v2/bookings/%s".formatted(cbrr),bookingData);

    addOperatorLogEntry(
      "Sent an updated confirmed booking with the parameters: %s"
        .formatted(cbrr));
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
}
