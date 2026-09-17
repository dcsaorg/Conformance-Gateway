package org.dcsa.conformance.end.party;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.end.action.SupplyScenarioParametersAction;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class EndorsementChainProvider extends ConformanceParty {
  public EndorsementChainProvider(
    String apiVersion,
    PartyConfiguration partyConfiguration,
    CounterpartConfiguration counterpartConfiguration,
    JsonNodeMap persistentMap,
    PartyWebClient webClient,
    Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {

  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {

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
    log.info(
      "EndorsementChainProvider.supplyScenarioParameters(%s)"
        .formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
      SuppliedScenarioParameters.fromMap(
        StreamSupport.stream(
            actionPrompt
              .required("endorsementChainFilterParamQueryParamNames")
              .spliterator(),
            false)
          .map(
            jsonEndorsementChainFilterParam ->
              EndorsementChainFilterParameter.byParamName.get(
                jsonEndorsementChainFilterParam.asText()))
          .collect(
            Collectors.toMap(
              Function.identity(),
              endorsementChainFilterParam ->
                switch (endorsementChainFilterParam) {
                  case EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE -> "HHL71800000";
                  case EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_SUB_REFERENCE ->
                    "fc5009a7-25ad-4bb0-9892-4e2dea6bcdd9";
                  case EndorsementChainFilterParameter.CARRIER_SCAC_CODE -> "YMLU";
                })));

    asyncOrchestratorPostPartyInput(
      actionPrompt.required("actionId").asText(), responseSsp.toJson());

    addOperatorLogEntry(
      "Submitting SuppliedScenarioParameters: %s"
        .formatted(responseSsp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EndorsementChainProvider.handleRequest(%s)".formatted(request));

    String majorVersionDigits = apiVersion.split("\\.")[0] + "00";
    JsonNode jsonResponseBody = createResponseBody(request, majorVersionDigits);
    return request.createResponse(
      200,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(jsonResponseBody));
  }

  static JsonNode createResponseBody(ConformanceRequest request, String majorVersionDigits) {
    String transportDocumentReference = transportDocumentReferenceFrom(request);
    String transportDocumentSubReference =
      Optional.ofNullable(request.queryParams().get("transportDocumentSubReference")).stream()
        .flatMap(Collection::stream)
        .findFirst()
        .orElseGet(ReferenceGenerator::newReference);

    return JsonToolkit.templateFileToJsonNode(
      "/standards/end/messages/endorsementchain-api-%s-get-response.json"
        .formatted(majorVersionDigits),
      Map.of(
        "TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", transportDocumentReference,
        "TRANSPORT_DOCUMENT_SUB_REFERENCE_PLACEHOLDER", transportDocumentSubReference));
  }

  static String transportDocumentReferenceFrom(ConformanceRequest request) {
    String rawPath = URI.create(request.url()).getRawPath();
    String rawTdr = rawPath.substring(rawPath.lastIndexOf('/') + 1);
    return URI.create("/" + rawTdr).getPath().substring(1);
  }
}
