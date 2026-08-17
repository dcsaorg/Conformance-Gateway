package org.dcsa.conformance.standards.cs.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.cs.model.CsDateUtils;
import org.dcsa.conformance.standards.cs.party.CsFilterParameter;
import org.dcsa.conformance.standards.cs.party.SuppliedScenarioParameters;

@Getter
public class SupplyScenarioParametersAction extends CsAction {

  private final LinkedHashSet<CsFilterParameter> requiredCsFilterParameters;
  private final LinkedHashSet<CsFilterParameter> optionalCsFilterParameters;
  private SuppliedScenarioParameters suppliedScenarioParameters = null;


  public SupplyScenarioParametersAction(
      String publisherPartyName, CsFilterParameter... csFilterParameters) {
    this(publisherPartyName, csFilterParameters, new CsFilterParameter[0]);
  }

  public SupplyScenarioParametersAction(
      String publisherPartyName,
      CsFilterParameter[] requiredParams,
      CsFilterParameter[] optionalParams) {
    super(
        publisherPartyName,
        null,
        null,
        "SupplyScenarioParameters(%s%s)"
            .formatted(
                Arrays.stream(requiredParams)
                    .map(CsFilterParameter::getQueryParamName)
                    .collect(Collectors.joining(", ")),
                optionalParams.length > 0 ? " [+ optional filters]" : ""),
        -1);
    this.requiredCsFilterParameters = new LinkedHashSet<>(Arrays.asList(requiredParams));
    this.optionalCsFilterParameters = new LinkedHashSet<>(Arrays.asList(optionalParams));
  }

  /** Returns all parameters (required + optional) in declaration order. */
  public LinkedHashSet<CsFilterParameter> getCsFilterParameters() {
    LinkedHashSet<CsFilterParameter> all = new LinkedHashSet<>(requiredCsFilterParameters);
    all.addAll(optionalCsFilterParameters);
    return all;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (suppliedScenarioParameters != null) {
      jsonState.set("suppliedScenarioParameters", suppliedScenarioParameters.toJson());
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
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode objectNode = super.asJsonNode();
    // Required parameters
    ArrayNode requiredArray = objectNode.putArray("requiredCsFilterParameterNames");
    requiredCsFilterParameters.forEach(p -> requiredArray.add(p.getQueryParamName()));
    // Optional parameters
    ArrayNode optionalArray = objectNode.putArray("optionalCsFilterParameterNames");
    optionalCsFilterParameters.forEach(p -> optionalArray.add(p.getQueryParamName()));
    // Combined list for backward compatibility
    ArrayNode allArray = objectNode.putArray("csFilterParametersQueryParamNames");
    getCsFilterParameters().forEach(p -> allArray.add(p.getQueryParamName()));
    return objectNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (optionalCsFilterParameters.isEmpty()) {
      return getMarkdownHumanReadablePrompt(null, "prompt-publisher-ssp.md");
    }
    return getMarkdownHumanReadablePrompt(null, "prompt-publisher-ssp-optional.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    LinkedHashMap<CsFilterParameter, String> allParams = new LinkedHashMap<>();
    for (CsFilterParameter p : requiredCsFilterParameters) {
      allParams.put(p, defaultValue(p));
    }
    for (CsFilterParameter p : optionalCsFilterParameters) {
      allParams.put(p, defaultValue(p));
    }
    return SuppliedScenarioParameters.fromMap(allParams).toJson();
  }

  private String defaultValue(CsFilterParameter p) {
    return switch (p) {
      case DATE, DEPARTURE_START_DATE, ARRIVAL_START_DATE, START_DATE ->
          CsDateUtils.getCurrentDate();
      case DEPARTURE_END_DATE, ARRIVAL_END_DATE, END_DATE -> CsDateUtils.getEndDateAfter3Months();
      case CARGO_TYPE -> "FCL";
      case LIMIT -> "1";
      case RESPONSE_SCOPE -> "FULL_VOYAGE";
      default -> "TODO";
    };
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode inputNode = partyInput.get("input");
    List<String> missingRequired =
        requiredCsFilterParameters.stream()
            .filter(
                p ->
                    !inputNode.has(p.getQueryParamName())
                        || inputNode.get(p.getQueryParamName()).asText().isBlank())
            .map(CsFilterParameter::getQueryParamName)
            .collect(Collectors.toList());
    if (!missingRequired.isEmpty()) {
      throw new UserFacingException(
          "The following required query parameters are missing or blank: "
              + String.join(", ", missingRequired));
    }
    suppliedScenarioParameters = SuppliedScenarioParameters.fromJson(inputNode);
  }

  @Override
  public void reset() {
    super.reset();
    suppliedScenarioParameters = null;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }
}
