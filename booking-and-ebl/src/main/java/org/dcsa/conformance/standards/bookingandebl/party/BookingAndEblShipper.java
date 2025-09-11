package org.dcsa.conformance.standards.bookingandebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.party.BookingShipper;
import org.dcsa.conformance.standards.ebl.party.EblShipper;

@Slf4j
public class BookingAndEblShipper extends ConformanceParty {

  private final BookingShipper bookingShipper;
  private final EblShipper eblShipper;

  public BookingAndEblShipper(
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
    String[] versions = apiVersion.split("-\\+-");
    this.bookingShipper =
        new BookingShipper(
            versions[0],
            partyConfiguration,
            counterpartConfiguration,
            persistentMap,
            webClient,
            orchestratorAuthHeader);
    this.eblShipper =
        new EblShipper(
            versions[1],
            partyConfiguration,
            counterpartConfiguration,
            persistentMap,
            webClient,
            orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    bookingShipper.exportPartyJsonState(targetObjectNode);
    eblShipper.exportPartyJsonState(targetObjectNode);
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    bookingShipper.importPartyJsonState(sourceObjectNode);
    eblShipper.importPartyJsonState(sourceObjectNode);
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    String requestUrl = request.url();

    if (isBookingRequest(requestUrl)) {
      log.debug("Routing request to Booking shipper: {}", request);
      return bookingShipper.handleRequest(request);
    }
    if (isEblRequest(requestUrl)) {
      log.debug("Routing request to EBL shipper: {}", request);
      return eblShipper.handleRequest(request);
    }

    return invalidRequest(request, 404, "The request did not match any known URL");
  }

  @Override
  protected void doReset() {
    bookingShipper.doReset();
    eblShipper.doReset();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> handlers = new HashMap<>();

    handlers.putAll(bookingShipper.getActionPromptHandlers());
    handlers.putAll(eblShipper.getActionPromptHandlers());

    return handlers;
  }

  private boolean isBookingRequest(String url) {
    return BookingShipper.BOOKING_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }

  private boolean isEblRequest(String url) {
    return EblShipper.EBL_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }
}
