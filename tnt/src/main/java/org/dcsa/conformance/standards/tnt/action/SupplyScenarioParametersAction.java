package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;

@Getter
public class SupplyScenarioParametersAction extends TntAction {
  private SuppliedScenarioParameters suppliedScenarioParameters = null;
  private final LinkedHashSet<TntFilterParameter> tntFilterParameters;
  private Boolean isBadRequest;
  private static final String IS_BAD_REQUEST = "isBadRequest";

  public SupplyScenarioParametersAction(
      String publisherPartyName,TntFilterParameter... tntFilterParameters) {
    super(publisherPartyName,null, null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          Arrays.stream(tntFilterParameters)
            .map(TntFilterParameter::getQueryParamName)
            .collect(Collectors.joining(", "))), -1 );
    this.tntFilterParameters = new LinkedHashSet<>(Arrays.asList(tntFilterParameters));
    this.isBadRequest = false;
  }

  public SupplyScenarioParametersAction(
    Boolean isBadRequest, String publisherPartyName,TntFilterParameter... tntFilterParameters) {
    super(publisherPartyName,null, null,
      "SupplyScenarioParameters(%s)"
        .formatted(
          Arrays.stream(tntFilterParameters)
            .map(TntFilterParameter::getQueryParamName)
            .collect(Collectors.joining(", "))), -1 );
    this.tntFilterParameters = new LinkedHashSet<>(Arrays.asList(tntFilterParameters));
    this.isBadRequest = isBadRequest;
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set("suppliedScenarioParameters", suppliedScenarioParameters.toJson());
    }
    if (isBadRequest != null) {
      jsonState.put(IS_BAD_REQUEST, isBadRequest);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has("suppliedScenarioParameters")) {
      suppliedScenarioParameters =
          SuppliedScenarioParameters.fromJson(jsonState.required("suppliedScenarioParameters"));
    }
    if (jsonState.has(IS_BAD_REQUEST)) {
      isBadRequest = jsonState.get(IS_BAD_REQUEST).asBoolean();
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    ArrayNode jsonTntFilterParameters = objectNode.putArray("tntFilterParametersQueryParamNames");
    tntFilterParameters.forEach(
        TntFilterParameter -> jsonTntFilterParameters.add(TntFilterParameter.getQueryParamName()));
    if (isBadRequest != null) {
      objectNode.put(IS_BAD_REQUEST, isBadRequest);
    }
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Use the following format to provide the values of the specified query parameters"
        + " for which your party can successfully process a GET request:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return SuppliedScenarioParameters.fromMap(
            tntFilterParameters.stream()
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        tntFilterParameter ->
                            switch (tntFilterParameter) {
                              case LIMIT -> "100";
                              case EVENT_CREATED_DATE_TIME,
                                      EVENT_CREATED_DATE_TIME_EQ,
                                      EVENT_CREATED_DATE_TIME_GT,
                                      EVENT_CREATED_DATE_TIME_GTE,
                                      EVENT_CREATED_DATE_TIME_LT,
                                      EVENT_CREATED_DATE_TIME_LTE ->
                                "2021-01-09T14:12:56+01:00";
                              case EVENT_TYPE -> "SHIPMENT";
                              case SHIPMENT_EVENT_TYPE_CODE -> {
                                if (isBadRequest != null && isBadRequest) {
                                  yield "INVALID_CODE";
                                } else {
                                  yield "DRFT";
                                }
                              }
                              case DOCUMENT_TYPE_CODE -> {
                                if (isBadRequest != null && isBadRequest) {
                                  yield "INVALID_CODE";
                                } else {
                                  yield "CBR";
                                }
                              }
                              case CARRIER_BOOKING_REFERENCE -> "CBR709951";
                              case TRANSPORT_DOCUMENT_REFERENCE -> "TDR709951";

                              default -> "TODO";
                            })))
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(partyInput.get("input"));
  }

}
