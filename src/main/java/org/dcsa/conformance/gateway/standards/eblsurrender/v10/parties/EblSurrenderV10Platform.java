package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.RequestSurrenderForDeliveryAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

public class EblSurrenderV10Platform extends ConformanceParty {
  public EblSurrenderV10Platform(
      String name,
      boolean internal,
      String gatewayBaseUrl,
      String gatewayRootPath,
      Function<String, JsonNode> partyPromptGetter,
      Consumer<JsonNode> partyInputConsumer) {
    super(name, internal, gatewayBaseUrl, gatewayRootPath, partyPromptGetter, partyInputConsumer);
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(RequestSurrenderForDeliveryAction.class, this::requestSurrenderForDelivery));
  }

  private void requestSurrenderForDelivery(JsonNode actionPrompt) {
    WebTestClient.bindToServer()
        .baseUrl(gatewayBaseUrl)
        .build()
        .post()
        .uri(gatewayRootPath + "/v1/surrender-requests")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Mono.just(
                new ObjectMapper()
                    .createObjectNode()
                    .put("surrenderRequestReference", UUID.randomUUID().toString())
                    .put("transportDocumentReference", actionPrompt.get("tdr").asText())
                    .put("surrenderRequestCode", "SREQ")),
            String.class)
        .exchange();
  }
}
