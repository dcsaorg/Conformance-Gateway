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
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.Shipper_GetBookingAction;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;

@Slf4j
public class Shipper extends ConformanceParty {
  public Shipper(
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
        Map.entry(Shipper_GetBookingAction.class, this::getBookingRequest));
  }

  private void getBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.getBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String cbrr = actionPrompt.get("cbrr").asText();

    asyncCounterpartGet("/v2/bookings/" + cbrr);

    addOperatorLogEntry("Sent a GET request for booking with CBRR: %s".formatted(cbrr));
  }

  private void sendBookingRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    CarrierScenarioParameters carrierScenarioParameters =
        CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));

    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/booking/messages/booking-v20-request.json",
            Map.ofEntries(
                Map.entry(
                    "CARRIER_SERVICE_NAME_PLACEHOLDER",
                    carrierScenarioParameters.carrierServiceName()),
                Map.entry(
                    "VESSEL_IMO_NUMBER_PLACEHOLDER", carrierScenarioParameters.vesselIMONumber())));

    asyncCounterpartPost("/v2/bookings", jsonRequestBody);

    addOperatorLogEntry(
        "Sent a booking request with the parameters: %s"
            .formatted(carrierScenarioParameters.toJson()));
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