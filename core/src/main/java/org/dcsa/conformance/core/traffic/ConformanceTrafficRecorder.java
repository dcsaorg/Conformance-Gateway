package org.dcsa.conformance.core.traffic;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConformanceTrafficRecorder {

  private final LinkedHashMap<UUID, ConformanceExchange> traffic = new LinkedHashMap<>();

  public void reset() {
    traffic.clear();
  }

  public Stream<ConformanceExchange> getTrafficStream() {
    return this.traffic.values().stream();
  }

  public synchronized void recordExchange(ConformanceExchange conformanceExchange) {
    this.traffic.put(conformanceExchange.getUuid(), conformanceExchange);
  }
}
