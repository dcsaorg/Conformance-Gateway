package org.dcsa.conformance.gateway.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.UUID;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.MultiValueMap;

@ToString
@Getter
public class ConformanceExchange {
  private final UUID uuid;
  private final ConformanceRequest request;
  private final ConformanceResponse response;

  private ConformanceExchange(
      String sourcePartyName,
      String sourcePartyRole,
      String targetPartyName,
      String targetPartyRole,
      UUID uuid,
      String method,
      String path,
      MultiValueMap<String, String> queryParams,
      MultiValueMap<String, String> requestHeaders,
      String requestBody) {
    this.uuid = uuid;
    this.request =
        new ConformanceRequest(
            method,
            path,
            queryParams,
            new ConformanceMessage(
                sourcePartyName,
                sourcePartyRole,
                targetPartyName,
                targetPartyRole,
                requestHeaders,
                requestBody,
                _parsedStringOrJsonError(requestBody),
                System.currentTimeMillis()));
    this.response = null;
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

  private ConformanceExchange(
      ConformanceExchange conformanceExchange,
      int statusCode,
      MultiValueMap<String, String> responseHeaders,
      String responseBody) {
    this.uuid = conformanceExchange.uuid;
    this.request =
        new ConformanceRequest(
            conformanceExchange.request.method(),
            conformanceExchange.request.path(),
            conformanceExchange.request.queryParams(),
            conformanceExchange.request.message());
    this.response =
        new ConformanceResponse(
            statusCode,
            new ConformanceMessage(
                conformanceExchange.request.message().targetPartyName(),
                conformanceExchange.request.message().targetPartyRole(),
                conformanceExchange.request.message().sourcePartyName(),
                conformanceExchange.request.message().sourcePartyRole(),
                responseHeaders,
                responseBody,
                _parsedStringOrJsonError(responseBody),
                System.currentTimeMillis()));
  }

  public static ConformanceExchange createFromRequest(
      String sourcePartyName,
      String sourcePartyRole,
      String targetPartyName,
      String targetPartyRole,
      UUID uuid,
      String httpMethod,
      String requestPath,
      MultiValueMap<String, String> requestQueryParams,
      MultiValueMap<String, String> requestHeaders,
      String requestBody) {
    return new ConformanceExchange(
        sourcePartyName,
        sourcePartyRole,
        targetPartyName,
        targetPartyRole,
        uuid,
        httpMethod,
        requestPath,
        requestQueryParams,
        requestHeaders,
        requestBody);
  }

  public ConformanceExchange mutateWithResponse(
      int responseStatusCode, MultiValueMap<String, String> responseHeaders, String responseBody) {
    return new ConformanceExchange(this, responseStatusCode, responseHeaders, responseBody);
  }
}
