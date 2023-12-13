package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.ToString;

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
      jsonBody = new ObjectMapper().readTree(this.stringBody);
      isCorrectJson = true;
    } catch (JsonProcessingException e) {
      jsonBody = new ObjectMapper().createObjectNode();
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
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode objectNode = objectMapper.createObjectNode();
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
