package org.dcsa.conformance.core.check;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
public class ConformanceCheck {
  protected final String title;

  private List<ConformanceCheck> _subChecks;

  private final List<ConformanceResult> results = new ArrayList<>();

  public ConformanceCheck(String title) {
    this.title = title;
  }

  synchronized private List<ConformanceCheck> getSubChecks() {
    if (_subChecks == null) {
      this._subChecks = this.createSubChecks().collect(Collectors.toList());
    }
    return _subChecks;
  }

  public final void check(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    List<ConformanceCheck> subChecks = getSubChecks();
    if (subChecks.isEmpty()) {
      this.doCheck(getExchangeByUuid);
    } else {
      subChecks.forEach(subCheck -> subCheck.check(getExchangeByUuid));
    }
  }

  protected void doCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {}

  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.empty();
  }

  public Stream<ConformanceCheck> subChecksStream() {
    return getSubChecks().stream();
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

  public Consumer<ConformanceStatus> computedStatusConsumer() {
    return ignoredStatus -> {};
  }
}
