package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.ToString;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
@ToString
public class ConformanceMessageBody {
  private final boolean isCorrectJson;
  private final String stringBody;
  private final JsonNode jsonBody;

  public ConformanceMessageBody(String stringBody) {
    this.stringBody = stringBody == null ? "" : stringBody;
    boolean isCorrectJson;
    JsonNode jsonBody;
    try {
      jsonBody = OBJECT_MAPPER.readTree(this.stringBody);
      isCorrectJson = true;
      // FIXME start of temporary workaround for SD-1942
      String overwritingValue = "Value overwritten by the DCSA Conformance sandbox as a workaround for SD-1942";
      JsonNode termsAndConditionsParent =
          JsonToolkit.findNodeWithAttribute(jsonBody, "termsAndConditions");
      if (termsAndConditionsParent != null) {
        ((ObjectNode) termsAndConditionsParent).put("termsAndConditions", overwritingValue);
      }
      JsonNode eBLVisualisationByCarrierParent =
          JsonToolkit.findNodeWithAttribute(jsonBody, "eBLVisualisationByCarrier");
      if (eBLVisualisationByCarrierParent != null) {
        JsonNode eBLVisualisationByCarrierNode =
            eBLVisualisationByCarrierParent.path("eBLVisualisationByCarrier");
        if (eBLVisualisationByCarrierNode.has("content")) {
          ((ObjectNode) eBLVisualisationByCarrierNode).put("content", overwritingValue);
        }
      }
      // FIXME end of temporary workaround
    } catch (JsonProcessingException e) {
      jsonBody = OBJECT_MAPPER.createObjectNode();
      isCorrectJson = false;
    }
    this.isCorrectJson = isCorrectJson;
    this.jsonBody = jsonBody;
  }

  public ConformanceMessageBody(JsonNode jsonBody) {
    this.isCorrectJson = true;
    this.jsonBody = jsonBody;
    this.stringBody = jsonBody.toPrettyString();
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    objectNode.put("isCorrectJson", isCorrectJson);
    if (isCorrectJson) {
      objectNode.set("jsonBody", jsonBody);
    } else {
      objectNode.put("stringBody", stringBody);
    }
    return objectNode;
  }

  public static ConformanceMessageBody fromJson(ObjectNode objectNode) {
    boolean isCorrectJson = objectNode.get("isCorrectJson").asBoolean();
    if (isCorrectJson) {
      return new ConformanceMessageBody(objectNode.get("jsonBody"));
    } else {
      return new ConformanceMessageBody(objectNode.get("stringBody").asText());
    }
  }
}
