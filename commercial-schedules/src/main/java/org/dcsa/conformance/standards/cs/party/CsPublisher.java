package org.dcsa.conformance.standards.cs.party;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
import org.dcsa.conformance.standards.cs.model.DateUtils;

@Slf4j
public class CsPublisher extends ConformanceParty {


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

    JsonNode jsonResponseBody = replacePlaceHolders(filePath, request);
    return request.createResponse(
      200,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(jsonResponseBody));
  }

  private JsonNode replacePlaceHolders(String filePath, ConformanceRequest request) {
    Map<String, String> entryMap = new HashMap<>();
    if (request.url().endsWith("v1/point-to-point-routes")) {

      Optional<String> arrivalStartDate = extractValue(request.queryParams(), "arrivalStartDate");
      Optional<String> arrivalEndDate = extractValue(request.queryParams(), "arrivalEndDate");
      Optional<String> departureStartDate =
          extractValue(request.queryParams(), "departureStartDate");
      Optional<String> departureEndDate = extractValue(request.queryParams(), "departureEndDate");

      if (arrivalStartDate.isPresent() || arrivalEndDate.isPresent()) {
        String startDate = arrivalStartDate.orElse(null);
        String endDate = arrivalEndDate.orElse(null);
        entryMap.put("ARRIVAL_DATE", getArrivalDate(startDate, endDate));
      } else {
        entryMap.put("ARRIVAL_DATE", DateUtils.DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
      }
      if (departureStartDate.isPresent() || departureEndDate.isPresent()) {
        String startDate = departureStartDate.orElse(null);
        String endDate = departureEndDate.orElse(null);
        entryMap.put("DEPARTURE_DATE", getDepartureDate(startDate, endDate));
      } else {
        entryMap.put("DEPARTURE_DATE", DateUtils.DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
      }
      return JsonToolkit.templateFileToJsonNode(filePath, entryMap);
    } else if (request.url().endsWith("v1/port-schedules")) {
      Optional<String> date = extractValue(request.queryParams(), "date");
      if (date.isPresent()) {
        entryMap.put("DATE", processDate(date.get(), "", "date"));
      } else {
        entryMap.put("DATE", DateUtils.DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
      }
      return JsonToolkit.templateFileToJsonNode(filePath, entryMap);
    }
    return JsonToolkit.templateFileToJsonNode(filePath, entryMap);
  }

  private Optional<String> extractValue(
      Map<String, ? extends Collection<String>> queryParams, String key) {
    return Optional.ofNullable(queryParams.get(key))
        .flatMap(collection -> collection.stream().findFirst());
  }

  private String getArrivalDate(String arrivalStartDate, String arrivalEndDate) {
    if (arrivalStartDate != null && arrivalEndDate != null) {
      return processDate(arrivalStartDate, arrivalEndDate, "range");
    }
    if (arrivalStartDate != null) {
      return processDate(arrivalStartDate, "", "startDate");
    }
    if (arrivalEndDate != null) {
      return processDate("", arrivalEndDate, "endDate");
    }
    return "";
  }

  private String getDepartureDate(String departureStartDate, String departureEndDate) {
    if (departureStartDate != null && departureEndDate != null) {
      return processDate(departureStartDate, departureEndDate, "range");
    }
    if (departureStartDate != null) {
      return processDate(departureStartDate, "", "startDate");
    }
    if (departureEndDate != null) {
      return processDate("", departureEndDate, "endDate");
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
    if (type.equals("date")) {
      LocalDate date = LocalDate.parse(startDate, formatter).plusDays(1);
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
                              case UN_LOCATION_CODE, PLACE_OF_RECEIPT, PLACE_OF_DELIVERY -> "NLAMS";
                              case FACILITY_SMDG_CODE -> "APM";
                              case VESSEL_OPERATOR_CARRIER_CODE -> "MAEU";
                              case DATE, DEPARTURE_START_DATE, ARRIVAL_START_DATE ->
                                  DateUtils.DATE_FORMAT.format(new Date());
                              case DEPARTURE_END_DATE, ARRIVAL_END_DATE -> DateUtils.getEndDate();
                              case MAX_TRANSHIPMENT -> "1";
                              case RECEIPT_TYPE_AT_ORIGIN, DELIVERY_TYPE_AT_DESTINATION -> "CY";
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


}
