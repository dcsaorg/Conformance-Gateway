package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestResponseAction;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class EblSurrenderPlatform extends ConformanceParty {

  public EblSurrenderPlatform(
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
    return Map.of(SurrenderRequestResponseAction.class, this::requestSurrender);
  }

  private void requestSurrender(JsonNode actionPrompt) {
    SuppliedScenarioParameters ssp = SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));
    String srr = ReferenceGenerator.newReference();
    String tdr = ssp.transportDocumentReference();
    boolean forAmendment = actionPrompt.get("forAmendment").booleanValue();
    String src = forAmendment ? "AREQ" : "SREQ";
    JsonNode jsonRequestBody = createSurrenderRequestBody(apiVersion, ssp, srr, forAmendment, Instant.now());

    syncCounterpartPost("/v%s/ebl-surrender-requests".formatted(apiVersion.charAt(0)), jsonRequestBody);

    addOperatorLogEntry("Sent surrender request with surrenderRequestCode '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s'"
      .formatted(src, srr, tdr));
  }

  static JsonNode createSurrenderRequestBody(
    String apiVersion,
    SuppliedScenarioParameters ssp,
    String surrenderRequestReference,
    boolean forAmendment,
    Instant surrenderActionDateTime) {
    return JsonToolkit.templateFileToJsonNode("/standards/eblsurrender/messages/eblsurrender-api-v%s-request.json".formatted(apiVersion),
      Map.ofEntries(
        Map.entry("SURRENDER_REQUEST_REFERENCE_PLACEHOLDER", surrenderRequestReference),
        Map.entry(
          "TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER",
          ssp.transportDocumentReference()),
        Map.entry(
          "SURRENDER_REQUEST_CODE_PLACEHOLDER", forAmendment ? "AREQ" : "SREQ"),
        Map.entry("ISSUE_TO_PARTY", ssp.issueToParty().toString()),
        Map.entry("SURRENDEREE_PARTY", ssp.surrendereeParty().toString()),
        Map.entry("CARRIER_PARTY", ssp.carrierParty().toString()),
        Map.entry(
          "ISSUE_ACTION_DATE_TIME_PLACEHOLDER",
          surrenderActionDateTime.minusSeconds(60).toString()),
        Map.entry(
          "SURRENDER_ACTION_DATE_TIME_PLACEHOLDER",
          surrenderActionDateTime.toString()),
        Map.entry(
          "SURRENDER_ACTION_CODE_PLACEHOLDER",
          forAmendment ? "SURRENDER_FOR_AMENDMENT" : "SURRENDER_FOR_DELIVERY")));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String action = jsonRequest.get("action").asText();
    String srr = jsonRequest.get("surrenderRequestReference").asText();

    addOperatorLogEntry(
      "Handling notification with action '%s' and surrenderRequestReference '%s'"
        .formatted(action, srr));
    return request.createResponse(
      204,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
