package org.dcsa.conformance.core.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.dcsa.conformance.core.report.ConformanceStatus;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Setter
@Getter
public abstract class ConformanceCheck {
  protected final String title;
  private List<ConformanceCheck> _subChecks;
  private boolean isApplicable = true;

  private final List<ConformanceResult> results = new ArrayList<>();

  protected ConformanceCheck(String title) {
    this.title = title;
  }

  private synchronized List<ConformanceCheck> getSubChecks() {
    if (_subChecks == null) {
      _subChecks = createSubChecks().filter(Objects::nonNull).collect(Collectors.toList());
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

  public boolean isApplicable() {
    if (results.stream().allMatch(Predicate.not(ConformanceResult::isApplicable))) {
      return false;
    }
    return isApplicable;
  }
}
