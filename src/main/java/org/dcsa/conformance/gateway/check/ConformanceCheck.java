package org.dcsa.conformance.gateway.check;

import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class ConformanceCheck {
  protected final String title;

  private final List<ConformanceCheck> subChecks;

  private final List<ConformanceResult> results = new ArrayList<>();

  public ConformanceCheck(String title) {
    this.title = title;
    this.subChecks = this.createSubChecks().collect(Collectors.toList());
  }

  public final void check(ConformanceExchange exchange) {
    if (this.subChecks.isEmpty()) {
      this.doCheck(exchange);
    } else {
      this.subChecks.forEach(subCheck -> subCheck.check(exchange));
    }
  }

  protected void doCheck(ConformanceExchange exchange) {}

  protected Stream<ConformanceCheck> createSubChecks() {
    return Stream.empty();
  }

  public Stream<ConformanceCheck> subChecksStream() {
    return subChecks.stream();
  }

  protected void addResult(ConformanceResult result) {
    this.results.add(result);
  }

  public Stream<ConformanceResult> resultsStream() {
    return results.stream();
  }

  public boolean isRelevantForRole(String roleName) {
    return true;
  }
}
