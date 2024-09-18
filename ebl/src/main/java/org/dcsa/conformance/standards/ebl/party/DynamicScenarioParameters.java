package org.dcsa.conformance.standards.ebl.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;
import lombok.NonNull;
import lombok.With;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;

@With
public record DynamicScenarioParameters(
    @NonNull
    ScenarioType scenarioType,
    String shippingInstructionsReference,
    String transportDocumentReference,
    JsonNode shippingInstructions,
    JsonNode updatedShippingInstructions,
    boolean newTransportDocumentContent,
    JsonNode transportDocument,
    JsonNode previousTransportDocument) {
  public ObjectNode toJson() {
    var node = OBJECT_MAPPER.createObjectNode()
      .put("scenarioType", scenarioType.name())
      .put("shippingInstructionsReference", shippingInstructionsReference)
      .put("transportDocumentReference", transportDocumentReference)
      .put("newTransportDocumentContent", newTransportDocumentContent);
    node.replace("shippingInstructions", shippingInstructions);
    node.replace("updatedShippingInstructions", updatedShippingInstructions);
    node.replace("transportDocument", transportDocument);
    node.replace("previousTransportDocument", previousTransportDocument);
    return node;
  }

  private static <E extends Enum<E>> String serializeEnum(E v, Function<E, String> mapper) {
    if (v == null) {
      return null;
    }
    return mapper.apply(v);
  }

  private static <E> E readEnum(String value, Function<String, E> mapper) {
    if (value == null) {
      return null;
    }
    return mapper.apply(value);
  }

  public static DynamicScenarioParameters fromJson(JsonNode jsonNode) {
    return new DynamicScenarioParameters(
        readEnum(jsonNode.required("scenarioType").asText(), ScenarioType::valueOf),
        jsonNode.path("shippingInstructionsReference").asText(null),
        jsonNode.path("transportDocumentReference").asText(null),
        jsonNode.path("shippingInstructions"),
        jsonNode.path("updatedShippingInstructions"),
        jsonNode.path("newTransportDocumentContent").asBoolean(false),
        jsonNode.path("transportDocument"),
        jsonNode.path("previousTransportDocument")
    );
  }
}
