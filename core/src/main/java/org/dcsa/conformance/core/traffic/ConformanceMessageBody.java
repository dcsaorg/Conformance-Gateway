package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

@Getter
@ToString
public class ConformanceMessageBody {
  private final String stringBody;
  private final JsonNode jsonBody;

  public ConformanceMessageBody(String stringBody) {
    this.stringBody = stringBody;
    this.jsonBody = _parsedStringOrJsonError(stringBody);
  }

  public ConformanceMessageBody(JsonNode jsonBody) {
    this.jsonBody = jsonBody;
    this.stringBody = jsonBody.toPrettyString();
  }

  private static JsonNode _parsedStringOrJsonError(String string) {
    if (string == null) return new ObjectMapper().createObjectNode();
    try {
      return new ObjectMapper().readTree(string);
    } catch (JsonProcessingException e) {
      ObjectNode jsonException = new ObjectMapper().createObjectNode();
      jsonException.put("RequestBodyJsonDecodingException", e.toString());
      ArrayNode jsonStackTrace = new ObjectMapper().createArrayNode();
      jsonException.set("StackTrace", jsonStackTrace);
      Arrays.stream(e.getStackTrace())
          .forEach(stackTraceElement -> jsonStackTrace.add(stackTraceElement.toString()));
      return jsonException;
    }
  }
}
