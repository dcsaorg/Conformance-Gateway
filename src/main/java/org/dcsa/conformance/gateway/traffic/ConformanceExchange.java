package org.dcsa.conformance.gateway.traffic;

import lombok.Getter;
import org.springframework.util.MultiValueMap;

import java.util.UUID;

public class ConformanceExchange {
  @Getter private final String sourcePartyName;
  @Getter private final String sourcePartyRole;
  @Getter private final String targetPartyName;
  @Getter private final String targetPartyRole;
  @Getter private final UUID uuid;
  @Getter private final String httpMethod;
  @Getter private final String requestPath;
  @Getter private final MultiValueMap<String, String> requestQueryParams;
  @Getter private final MultiValueMap<String, String> requestHeaders;
  @Getter private final String requestBody;
  @Getter private final long requestTimestamp;
  @Getter private final MultiValueMap<String, String> responseHeaders;
  @Getter private final String responseBody;
  @Getter private final long responseTimestamp;

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
