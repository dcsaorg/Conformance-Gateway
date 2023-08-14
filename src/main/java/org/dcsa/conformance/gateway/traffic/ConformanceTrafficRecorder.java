package org.dcsa.conformance.gateway.traffic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
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
    log.info(">>>>>>>>>>>>>>>");
    log.info("Gateway request " + uuid);
    log.info(">>>>>>>>>>>>>>>");
    log.info("" + webExchange.getRequest().getMethod());
    log.info("" + webExchange.getRequest().getPath());
    log.info("" + webExchange.getRequest().getQueryParams());
    log.info(">>>>>>>>>>>>>>>");
    log.info("" + webExchange.getRequest().getHeaders());
    log.info(">>>>>>>>>>>>>>>");
    log.info(requestBody);
    log.info(">>>>>>>>>>>>>>>");

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
    log.info("<<<<<<<<<<<<<<<<");
    UUID uuid = webExchange.getAttribute(UUID_KEY);
    log.info("Gateway response " + uuid);
    log.info("<<<<<<<<<<<<<<<<");
    log.info("" + webExchange.getResponse().getHeaders());
    log.info("<<<<<<<<<<<<<<<<");
    log.info(responseBody);
    log.info("<<<<<<<<<<<<<<<<");
    this.traffic.put(
        uuid,
        this.traffic
            .get(uuid)
            .mutateWithResponse(webExchange.getResponse().getHeaders(), responseBody));
    log.info("Recorded %d exchanges".formatted(this.traffic.size()));
  }
}
