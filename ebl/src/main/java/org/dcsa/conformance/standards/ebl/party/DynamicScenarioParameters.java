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
    ShippingInstructionsStatus shippingInstructionsStatus,
    ShippingInstructionsStatus updatedShippingInstructionsStatus,
    TransportDocumentStatus transportDocumentStatus) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER.createObjectNode()
      .put("scenarioType", scenarioType.name())
      .put("shippingInstructionsReference", shippingInstructionsReference)
      .put("transportDocumentReference", transportDocumentReference)
      .put("shippingInstructionsStatus", serializeEnum(shippingInstructionsStatus, ShippingInstructionsStatus::wireName))
      .put("updatedShippingInstructionsStatus", serializeEnum(updatedShippingInstructionsStatus, ShippingInstructionsStatus::wireName))
      .put("transportDocumentStatus", serializeEnum(transportDocumentStatus, TransportDocumentStatus::wireName));
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
        readEnum(jsonNode.required("shippingInstructionsStatus").asText(null), ShippingInstructionsStatus::fromWireName),
        readEnum(jsonNode.required("updatedShippingInstructionsStatus").asText(null), ShippingInstructionsStatus::fromWireName),
        readEnum(jsonNode.required("transportDocumentStatus").asText(null), TransportDocumentStatus::fromWireName)
    );
  }
}
