package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10State;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.AcceptSurrenderForDeliveryAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.RejectSurrenderForDeliveryAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;

public class EblSurrenderV10Carrier extends ConformanceParty {
  private Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();

  public EblSurrenderV10Carrier(
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
        Map.entry(SupplyAvailableTdrAction.class, this::supplyAvailableTdr),
        Map.entry(AcceptSurrenderForDeliveryAction.class, actionPrompt -> {}),
        Map.entry(RejectSurrenderForDeliveryAction.class, actionPrompt -> {}));
  }

  private void supplyAvailableTdr(JsonNode actionPrompt) {
    String tdr = UUID.randomUUID().toString();
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    partyInputConsumer.accept(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .put("tdr", tdr));
  }
}
