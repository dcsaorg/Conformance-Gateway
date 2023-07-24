package org.dcsa.conformance.gateway;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConformanceCheck {
  @Getter protected final String title;

  protected final List<ConformanceResult> results = new ArrayList<>();

  public ConformanceCheck(String title) {
    this.title = title;
  }

  public final void check(ConformanceExchange exchange) {
    this.doCheck(exchange);
    this.getSubChecks().forEach(subCheck -> subCheck.check(exchange));
  }

  protected void doCheck(ConformanceExchange exchange) {}

  protected Stream<ConformanceCheck> getSubChecks() {
    return Stream.empty();
  }
}
