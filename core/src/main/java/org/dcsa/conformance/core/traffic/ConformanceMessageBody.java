package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ConformanceMessageBody {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Configuration JSON_PATH_CONFIGURATION =  Configuration.builder()
    .jsonProvider(new JacksonJsonProvider(OBJECT_MAPPER))
    .mappingProvider(new JacksonMappingProvider(OBJECT_MAPPER))
    .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
    .build();
  private final boolean isCorrectJson;
  private final String stringBody;
  private final JsonNode jsonBody;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Getter(lazy = true)
  private final DocumentContext jsonPathBody = JsonPath.parse(isCorrectJson ? stringBody : "{}", JSON_PATH_CONFIGURATION);

  public ConformanceMessageBody(String stringBody) {
    this.stringBody = stringBody;
    boolean isCorrectJson;
    JsonNode jsonBody;
    try {
      jsonBody = OBJECT_MAPPER.readTree(stringBody);
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
