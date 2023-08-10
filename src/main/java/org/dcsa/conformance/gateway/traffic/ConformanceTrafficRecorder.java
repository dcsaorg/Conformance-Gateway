package org.dcsa.conformance.gateway.traffic;

import org.springframework.web.server.ServerWebExchange;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;

public class ConformanceTrafficRecorder {
  private static final String UUID_KEY = ConformanceTrafficRecorder.class.getCanonicalName();

  private final LinkedHashMap<UUID, ConformanceExchange> traffic = new LinkedHashMap<>();

  public void reset() {
    traffic.clear();
  }

  public Stream<ConformanceExchange> getTrafficStream() {
    return this.traffic.values().stream();
  }

  public void recordRequest(
      String sourcePartyName,
      String sourcePartyRole,
      String targetPartyName,
      String targetPartyRole,
      ServerWebExchange webExchange,
      String requestBody) {
    UUID uuid = UUID.randomUUID();
    webExchange.getAttributes().put(UUID_KEY, uuid);
    System.out.println(">>>>>>>>>>>>>>>");
    System.out.println("Gateway request " + uuid);
    System.out.println(">>>>>>>>>>>>>>>");
    System.out.println(webExchange.getRequest().getMethod());
    System.out.println(webExchange.getRequest().getPath());
    System.out.println(webExchange.getRequest().getQueryParams());
    System.out.println(">>>>>>>>>>>>>>>");
    System.out.println(webExchange.getRequest().getHeaders());
    System.out.println(">>>>>>>>>>>>>>>");
    System.out.println(requestBody);
    System.out.println(">>>>>>>>>>>>>>>");

    this.traffic.put(
        uuid,
        ConformanceExchange.createFromRequest(
            sourcePartyName,
            sourcePartyRole,
            targetPartyName,
            targetPartyRole,
            uuid,
            webExchange.getRequest().getMethod().name(),
            webExchange.getRequest().getPath().value(),
            webExchange.getRequest().getQueryParams(),
            webExchange.getRequest().getHeaders(),
            requestBody));
  }

  public void recordResponse(ServerWebExchange webExchange, String responseBody) {
    System.out.println("<<<<<<<<<<<<<<<<");
    UUID uuid = webExchange.getAttribute(UUID_KEY);
    System.out.println("Gateway response " + uuid);
    System.out.println("<<<<<<<<<<<<<<<<");
    System.out.println(webExchange.getResponse().getHeaders());
    System.out.println("<<<<<<<<<<<<<<<<");
    System.out.println(responseBody);
    System.out.println("<<<<<<<<<<<<<<<<");
    this.traffic.put(
        uuid,
        this.traffic
            .get(uuid)
            .mutateWithResponse(webExchange.getResponse().getHeaders(), responseBody));
    System.out.println("Recorded %d exchanges".formatted(this.traffic.size()));
  }
}
