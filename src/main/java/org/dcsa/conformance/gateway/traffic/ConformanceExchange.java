package org.dcsa.conformance.gateway.traffic;

import lombok.Getter;
import lombok.ToString;
import org.springframework.util.MultiValueMap;

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
  private final long requestTimestamp;
  private final MultiValueMap<String, String> responseHeaders;
  private final String responseBody;
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
    this.sourcePartyRole = conformanceExchange.sourcePartyRole;
    this.targetPartyName = conformanceExchange.targetPartyName;
    this.targetPartyRole = conformanceExchange.targetPartyRole;
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
      MultiValueMap<String, String> responseHeaders, String responseBody) {
    return new ConformanceExchange(this, responseHeaders, responseBody);
  }
}
