package org.dcsa.conformance.gateway;

import lombok.Getter;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

public class ConformanceExchange {
  @Getter private final String sourcePartyName;
  @Getter private final String targetPartyName;
  @Getter private final UUID uuid;
  private final String httpMethod;
  private final String requestPath;
  private final MultiValueMap<String, String> requestQueryParams;
  private final MultiValueMap<String, String> requestHeaders;
  private final String requestBody;
  private final long requestTimestamp;
  private final MultiValueMap<String, String> responseHeaders;
  private final String responseBody;
  private final long responseTimestamp;

  private ConformanceExchange(
      String sourcePartyName,
      String targetPartyName,
      UUID uuid,
      String httpMethod,
      String requestPath,
      MultiValueMap<String, String> requestQueryParams,
      MultiValueMap<String, String> requestHeaders,
      String requestBody) {
    this.sourcePartyName = sourcePartyName;
    this.targetPartyName = targetPartyName;
    this.uuid = uuid;
    this.httpMethod = httpMethod;
    this.requestPath = requestPath;
    this.requestQueryParams = requestQueryParams;
    this.requestHeaders = requestHeaders;
    this.requestBody = requestBody;
    this.requestTimestamp = System.currentTimeMillis();
    this.responseHeaders = null;
    this.responseBody = null;
    this.responseTimestamp = 0L;
  }

  private ConformanceExchange(
      ConformanceExchange conformanceExchange,
      MultiValueMap<String, String> responseHeaders,
      String responseBody) {
    this.sourcePartyName = conformanceExchange.sourcePartyName;
    this.targetPartyName = conformanceExchange.targetPartyName;
    this.uuid = conformanceExchange.uuid;
    this.httpMethod = conformanceExchange.httpMethod;
    this.requestPath = conformanceExchange.requestPath;
    this.requestQueryParams = conformanceExchange.requestQueryParams;
    this.requestHeaders = conformanceExchange.requestHeaders;
    this.requestBody = conformanceExchange.requestBody;
    this.requestTimestamp = conformanceExchange.requestTimestamp;
    this.responseHeaders = responseHeaders;
    this.responseBody = responseBody;
    this.responseTimestamp = System.currentTimeMillis();
  }

  public static ConformanceExchange createFromRequest(
      String sourcePartyName,
      String targetPartyName,
      UUID uuid,
      String httpMethod,
      String requestPath,
      MultiValueMap<String, String> requestQueryParams,
      MultiValueMap<String, String> requestHeaders,
      String requestBody) {
    return new ConformanceExchange(
        sourcePartyName,
        targetPartyName,
        uuid,
        httpMethod,
        requestPath,
        requestQueryParams,
        requestHeaders,
        requestBody);
  }

  public ConformanceExchange mutateWithResponse(
      MultiValueMap<String, String> responseHeaders, String responseBody) {
    return new ConformanceExchange(this, responseHeaders, responseBody);
  }
}
