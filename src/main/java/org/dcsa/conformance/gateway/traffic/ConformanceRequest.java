package org.dcsa.conformance.gateway.traffic;


import java.util.Collection;
import java.util.Map;

public record ConformanceRequest(
    String method,
    String baseUrl,
    String path,
    Map<String, ? extends Collection<String>> queryParams,
    ConformanceMessage message) {

  public ConformanceResponse createResponse(
      int statusCode,
      Map<String, ? extends Collection<String>> headers,
      ConformanceMessageBody body) {
    return new ConformanceResponse(
        statusCode,
        new ConformanceMessage(
            this.message.targetPartyName(),
            this.message.targetPartyRole(),
            this.message.sourcePartyName(),
            this.message.sourcePartyRole(),
            headers,
            body,
            System.currentTimeMillis()));
  }
}
