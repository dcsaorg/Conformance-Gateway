package org.dcsa.conformance.standards.cs.party;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CsPublisher extends ConformanceParty {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  public CsPublisher(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {

  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {

  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("CsPublisher.handleRequest(%s)".formatted(request));

    String filePath;
    if(request.url().endsWith("v1/point-to-point-routes")){
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-ptp.json";
    }else if(request.url().endsWith("v1/port-schedules")){
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-ps.json";
    }else{
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-vs.json";
    }
    /*JsonNode jsonResponseBody = JsonToolkit.templateFileToJsonNode(filePath,
    Map.ofEntries());*/
    JsonNode jsonResponseBody = replacePlaceHolders(filePath, request.queryParams());
    return request.createResponse(
      200,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(jsonResponseBody));
  }

  private JsonNode replacePlaceHolders(
      String filePath, Map<String, ? extends Collection<String>> queryParams) {

    Optional<String> arrivalStartDate =
        Optional.ofNullable(queryParams.get("arrivalStartDate"))
            .flatMap(collection -> collection.stream().findFirst());

    Optional<String> arrivalEndDate =
        Optional.ofNullable(queryParams.get("arrivalEndDate"))
            .flatMap(collection -> collection.stream().findFirst());

    Optional<String> departureStartDate =
        Optional.ofNullable(queryParams.get("departureStartDate"))
            .flatMap(collection -> collection.stream().findFirst());

    Optional<String> departureEndDate =
        Optional.ofNullable(queryParams.get("departureEndDate"))
            .flatMap(collection -> collection.stream().findFirst());

    return JsonToolkit.templateFileToJsonNode(
        filePath,
        Map.ofEntries(
            Map.entry("ARRIVAL_DATE", getArrivalDate(arrivalStartDate, arrivalEndDate)),
            Map.entry("DEPARTURE_DATE", getDepartureDate(departureStartDate, departureEndDate))));
  }

  private String getArrivalDate(
      Optional<String> arrivalStartDate, Optional<String> arrivalEndDate) {
    if (arrivalStartDate.isPresent() && arrivalEndDate.isPresent()) {
      return processDate(arrivalStartDate.get(), arrivalEndDate.get(), "range");
    } else if (arrivalStartDate.isPresent() && !arrivalEndDate.isPresent()) {
      return processDate(arrivalStartDate.get(), "", "startDate");
    } else if (!arrivalStartDate.isPresent() && arrivalEndDate.isPresent()) {
      return processDate("", arrivalEndDate.get(), "endDate");
    }
    return "";
  }

  private String getDepartureDate(
      Optional<String> departureStartDate, Optional<String> departureEndDate) {
    if (departureStartDate.isPresent() && departureEndDate.isPresent()) {
      return processDate(departureStartDate.get(), departureEndDate.get(), "range");
    } else if (departureStartDate.isPresent() && !departureEndDate.isPresent()) {
      return processDate(departureStartDate.get(), "", "startDate");
    } else if (!departureStartDate.isPresent() && departureEndDate.isPresent()) {
      return processDate("", departureEndDate.get(), "endDate");
    }
    return "";
  }

  private String processDate(String startDate, String endDate, String type) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    if (type.equals("startDate")) {
      LocalDate date = LocalDate.parse(startDate, formatter).plusWeeks(1);
      return convertDateToDateTime(date);
    }
    if (type.equals("endDate")) {
      LocalDate date = LocalDate.parse(endDate, formatter).minusWeeks(1);
      return convertDateToDateTime(date);
    }
    if (type.equals("range")) {
      LocalDate date = LocalDate.parse(startDate, formatter).plusWeeks(1);
      return convertDateToDateTime(date);
    }
    return "";
  }

  private String convertDateToDateTime(LocalDate date) {
    LocalDateTime dateTime = date.atStartOfDay();
    ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    return zonedDateTime.format(dateTimeFormatter);
  }

  @Override
  protected void doReset() {

  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("CsPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
        SuppliedScenarioParameters.fromMap(
            StreamSupport.stream(
                    actionPrompt.required("csFilterParametersQueryParamNames").spliterator(), false)
                .map(
                    jsonCsFilterParameter ->
                        CsFilterParameter.byQueryParamName.get(jsonCsFilterParameter.asText()))
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        csFilterParameter ->
                            switch (csFilterParameter) {
                              case CARRIER_SERVICE_NAME -> "Great Lion Service";
                              case CARRIER_SERVICE_CODE -> "FE1";
                              case UNIVERSAL_SERVICE_REFERENCE -> "SR12345A";
                              case VESSEL_IMO_NUMBER -> "9321483";
                              case VESSEL_NAME -> "King of the Seas";
                              case CARRIER_VOYAGE_NUMBER -> "2103S";
                              case UNIVERSAL_VOYAGE_REFERENCE -> "2201N";
                              case UN_LOCATION_CODE -> "NLAMS";
                              case FACILITY_SMDG_CODE -> "APM";
                              case VESSEL_OPERATOR_CARRIER_CODE -> "MAEU";
                              case DATE -> DATE_FORMAT.format(new Date());
                              case PLACE_OF_RECEIPT -> "NLAMS";
                              case PLACE_OF_DELIVERY -> "NLAMS";
                              case DEPARTURE_START_DATE -> DATE_FORMAT.format(new Date());
                              case DEPARTURE_END_DATE -> getEndDate();
                              case ARRIVAL_START_DATE -> DATE_FORMAT.format(new Date());
                              case ARRIVAL_END_DATE -> getEndDate();
                              case MAX_TRANSHIPMENT -> "1";
                              case RECEIPT_TYPE_AT_ORIGIN -> "CY";
                              case DELIVERY_TYPE_AT_DESTINATION -> "CY";
                            })));

    asyncOrchestratorPostPartyInput(
      new ObjectMapper()
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText())
        .set("input", responseSsp.toJson()));

    addOperatorLogEntry(
      "Submitting SuppliedScenarioParameters: %s"
        .formatted(responseSsp.toJson().toPrettyString()));
  }

  private String getEndDate() {
    Date currentDate = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(currentDate);
    calendar.add(Calendar.MONTH, 3);
    return DATE_FORMAT.format(calendar.getTime());
  }
}
