package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class EblSurrenderCarrier extends ConformanceParty {

  public EblSurrenderCarrier(
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
    return Map.of(SupplyScenarioParametersAction.class, this::supplyScenarioParameters);
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    String tdr = ReferenceGenerator.newReference();

    var identifyingCodes = OBJECT_MAPPER
      .createArrayNode()
      .add(
        OBJECT_MAPPER
          .createObjectNode()
          .put("codeListProvider", "WAVE")
          .put("partyCode", "MSK")
          .put("codeListName", "DID"));

    var issueToParty = OBJECT_MAPPER.createObjectNode();
    issueToParty.set("identifyingCodes", identifyingCodes);
    issueToParty.put("partyName", "Issue To name").put("eblPlatform", "WAVE");
    var carrierParty = OBJECT_MAPPER.createObjectNode();
    carrierParty.set("identifyingCodes", identifyingCodes);
    carrierParty.put("partyName", "Carrier name").put("eblPlatform", "WAVE");

    var surrendereeParty = OBJECT_MAPPER.createObjectNode();
    surrendereeParty.set("identifyingCodes", identifyingCodes);
    surrendereeParty.put("partyName", "Surrenderee name").put("eblPlatform", "BOLE");

    SuppliedScenarioParameters suppliedScenarioParameters =
      new SuppliedScenarioParameters(tdr, issueToParty, carrierParty, surrendereeParty);

    asyncOrchestratorPostPartyInput(
      actionPrompt.required("actionId").asText(), suppliedScenarioParameters.toJson());

    addOperatorLogEntry(
      "Submitting SuppliedScenarioParameters: %s"
        .formatted(suppliedScenarioParameters.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String src = jsonRequest.get("surrenderRequestCode").asText();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = jsonRequest.get("transportDocumentReference").asText();

    var carrierResponse = createSurrenderResponseBody(srr);

    asyncCounterpartNotification(null, "/v3/ebl-surrender-responses", carrierResponse);

    addOperatorLogEntry("Handling surrender request with surrenderRequestCode '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s'"
      .formatted(src, srr, tdr));

    return request.createResponse(
      204,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }

  static ObjectNode createSurrenderResponseBody(String surrenderRequestReference) {
    return OBJECT_MAPPER.createObjectNode()
      .put("surrenderRequestReference", surrenderRequestReference)
      .put("action", "SURR");
  }
}
