package org.dcsa.conformance.gateway.traffic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.UUID;

@ToString
@Getter
public class ConformanceExchange {
  private final String sourcePartyName;
  private final String sourcePartyRole;
  private final String targetPartyName;
  private final String targetPartyRole;
  private final UUID uuid;
  private final String httpMethod;
  private final String requestPath;
  private final MultiValueMap<String, String> requestQueryParams;
  private final MultiValueMap<String, String> requestHeaders;
  private final String requestBody;
  private final JsonNode jsonRequestBody;
  private final long requestTimestamp;
  private final int responseStatusCode;
  private final MultiValueMap<String, String> responseHeaders;
  private final String responseBody;
  private final JsonNode jsonResponseBody;
  private final long responseTimestamp;

  private ConformanceExchange(
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
    this.sourcePartyName = sourcePartyName;
    this.sourcePartyRole = sourcePartyRole;
    this.targetPartyName = targetPartyName;
    this.targetPartyRole = targetPartyRole;
    this.uuid = uuid;
    this.httpMethod = httpMethod;
    this.requestPath = requestPath;
    this.requestQueryParams = requestQueryParams;
    this.requestHeaders = requestHeaders;
    this.requestBody = requestBody;
    this.jsonRequestBody = _parsedStringOrJsonError(requestBody);
    this.requestTimestamp = System.currentTimeMillis();
    this.responseStatusCode = 0;
    this.responseHeaders = null;
    this.responseBody = null;
    this.jsonResponseBody = null;
    this.responseTimestamp = 0L;
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
      int responseStatusCode,
      MultiValueMap<String, String> responseHeaders,
      String responseBody) {
    this.sourcePartyName = conformanceExchange.sourcePartyName;
    this.sourcePartyRole = conformanceExchange.sourcePartyRole;
    this.targetPartyName = conformanceExchange.targetPartyName;
    this.targetPartyRole = conformanceExchange.targetPartyRole;
    this.uuid = conformanceExchange.uuid;
    this.httpMethod = conformanceExchange.httpMethod;
    this.requestPath = conformanceExchange.requestPath;
    this.requestQueryParams = conformanceExchange.requestQueryParams;
    this.requestHeaders = conformanceExchange.requestHeaders;
    this.requestBody = conformanceExchange.requestBody;
    this.jsonRequestBody = _parsedStringOrJsonError(requestBody);
    this.requestTimestamp = conformanceExchange.requestTimestamp;
    this.responseStatusCode = responseStatusCode;
    this.responseHeaders = responseHeaders;
    this.responseBody = responseBody;
    this.jsonResponseBody = _parsedStringOrJsonError(responseBody);
    this.responseTimestamp = System.currentTimeMillis();
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
