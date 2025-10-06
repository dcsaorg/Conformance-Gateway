package org.dcsa.conformance.standards.bookingandebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.logs.TimestampedLogEntry;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.party.BookingCarrier;
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
      log.debug("Routing request to Booking carrier: {}", request);
      return bookingCarrier.handleRequest(request);
    }
    if (isEblRequest(requestUrl)) {
      log.debug("Routing request to EBL carrier: {}", request);
      return eblCarrier.handleRequest(request);
    }

    return invalidRequest(request, 404, "The request did not match any known URL");
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

  @Override
  public JsonNode exportJsonState() {
    JsonNode bookingState = this.bookingCarrier.exportJsonState();
    JsonNode eblState = this.eblCarrier.exportJsonState();

    ObjectNode combinedState = bookingState.deepCopy();

    if (eblState.isObject()) {
      eblState
          .fields()
          .forEachRemaining(field -> combinedState.set(field.getKey(), field.getValue()));
    }

    return combinedState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    this.bookingCarrier.importJsonState(jsonState);
    this.eblCarrier.importJsonState(jsonState);
  }

  @Override
  public List<TimestampedLogEntry> getOperatorLog() {
    List<TimestampedLogEntry> operatorLogs = new LinkedList<>();

    operatorLogs.addAll(this.bookingCarrier.getOperatorLog());
    operatorLogs.addAll(this.eblCarrier.getOperatorLog());

    operatorLogs.sort(Collections.reverseOrder());

    return operatorLogs;
  }

  private boolean isBookingRequest(String url) {
    return BookingCarrier.BOOKING_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }

  private boolean isEblRequest(String url) {
    return EblCarrier.EBL_ENDPOINT_PATTERNS.stream().anyMatch(url::matches);
  }
}
