package org.dcsa.conformance.core.traffic;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ConformanceMessageBody {
  private final boolean isRawByteArray;
  private final byte[] byteArrayBody;
  private final String stringBody;
  private final JsonNode jsonBody;
  private final boolean isCorrectJson;

  public ConformanceMessageBody(byte[] byteArrayBody) {
    this.isRawByteArray = true;
    this.byteArrayBody = byteArrayBody == null ? new byte[0] : byteArrayBody;
    this.stringBody = new String(this.byteArrayBody, StandardCharsets.UTF_8);
    boolean isCorrectJson;
    JsonNode jsonBody;
    try {
      jsonBody = OBJECT_MAPPER.readTree(this.stringBody);
      isCorrectJson = true;
    } catch (JsonProcessingException e) {
      jsonBody = OBJECT_MAPPER.createObjectNode();
      isCorrectJson = false;
    }
    this.isCorrectJson = isCorrectJson;
    this.jsonBody = jsonBody;
  }

  public ConformanceMessageBody(String stringBody) {
    this.isRawByteArray = false;
    this.stringBody = stringBody == null ? "" : stringBody;
    this.byteArrayBody = this.stringBody.getBytes(StandardCharsets.UTF_8);
    boolean isCorrectJson;
    JsonNode jsonBody;
    try {
      jsonBody = OBJECT_MAPPER.readTree(this.stringBody);
      isCorrectJson = true;
    } catch (JsonProcessingException e) {
      jsonBody = OBJECT_MAPPER.createObjectNode();
      isCorrectJson = false;
    }
    this.isCorrectJson = isCorrectJson;
    this.jsonBody = jsonBody;
  }

  public ConformanceMessageBody(JsonNode jsonBody) {
    this.isRawByteArray = false;
    this.isCorrectJson = true;
    this.jsonBody = jsonBody;
    this.stringBody = jsonBody.toPrettyString();
    this.byteArrayBody = this.stringBody.getBytes(StandardCharsets.UTF_8);
  }

  /* package local */ ObjectNode toJson() {
    ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
    objectNode.put("isRawByteArray", isRawByteArray);
    objectNode.put("isCorrectJson", isCorrectJson);
    if (isRawByteArray) {
      objectNode.put("byteArrayBody", Base64.getEncoder().encodeToString(byteArrayBody));
    } else if (isCorrectJson) {
      objectNode.set("jsonBody", jsonBody);
    } else {
      objectNode.put("stringBody", stringBody);
    }
    return objectNode;
  }

  /* package local */ static ConformanceMessageBody fromJson(ObjectNode objectNode) {
    boolean isRawByteArray = objectNode.path("isRawByteArray").asBoolean();
    if (isRawByteArray) {
      return new ConformanceMessageBody(Base64.getDecoder().decode(objectNode.get("byteArrayBody").asText()));
    }
    boolean isCorrectJson = objectNode.get("isCorrectJson").asBoolean();
    if (isCorrectJson) {
      return new ConformanceMessageBody(objectNode.get("jsonBody"));
    } else {
      return new ConformanceMessageBody(objectNode.get("stringBody").asText());
    }
  }
}
