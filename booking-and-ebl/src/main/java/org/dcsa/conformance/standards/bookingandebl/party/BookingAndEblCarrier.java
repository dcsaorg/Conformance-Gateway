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
import org.dcsa.conformance.standards.booking.BookingStandard;
import org.dcsa.conformance.standards.booking.party.BookingCarrier;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.ebl.party.EblCarrier;

@Slf4j
public class BookingAndEblCarrier extends ConformanceParty {

  private final BookingCarrier bookingCarrier;
  private final EblCarrier eblCarrier;
  
  public BookingAndEblCarrier(
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
    this.bookingCarrier =
        new BookingCarrier(
            versions[0],
            partyConfiguration,
            counterpartConfiguration,
            persistentMap,
            webClient,
            orchestratorAuthHeader);
    this.eblCarrier =
        new EblCarrier(
            versions[1],
            partyConfiguration,
            counterpartConfiguration,
            persistentMap,
            webClient,
            orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    bookingCarrier.exportPartyJsonState(targetObjectNode);
    eblCarrier.exportPartyJsonState(targetObjectNode);
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    bookingCarrier.importPartyJsonState(sourceObjectNode);
    eblCarrier.importPartyJsonState(sourceObjectNode);
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    String requestUrl = request.url();

    if (isBookingRequest(requestUrl)) {
      log.debug("Routing request to Booking carrier: {}", requestUrl);
      return bookingCarrier.handleRequest(request);
    }
    if (isEblRequest(requestUrl)) {
      log.debug("Routing request to EBL carrier: {}", requestUrl);
      return eblCarrier.handleRequest(request);
    }

    return bookingCarrier.return404(request);
  }

  @Override
  protected void doReset() {
    bookingCarrier.doReset();
    eblCarrier.doReset();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> handlers = new HashMap<>();

    handlers.putAll(bookingCarrier.getActionPromptHandlers());
    handlers.putAll(eblCarrier.getActionPromptHandlers());

    return handlers;
  }

  private boolean isBookingRequest(String url) {
    return BookingStandard.BOOKING_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }

  private boolean isEblRequest(String url) {
    return EblStandard.EBL_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }
}
