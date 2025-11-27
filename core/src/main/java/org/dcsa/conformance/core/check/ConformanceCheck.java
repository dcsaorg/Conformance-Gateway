package org.dcsa.conformance.core.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  private List<ConformanceCheck> subChecks;
  private final List<ConformanceResult> results = new ArrayList<>();

  private boolean isRelevant = true;
  private boolean isApplicable = true;

  protected ConformanceCheck(String title) {
    this.title = title;
  }

  private synchronized List<ConformanceCheck> getSubChecks() {
    if (subChecks == null) {
      subChecks = createSubChecks().collect(Collectors.toList());
    }
    return subChecks.stream()
        .filter(Objects::nonNull)
        .filter(ConformanceCheck::isApplicable)
        .toList();
  }

  public final void check(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    List<ConformanceCheck> conformanceChecks = getSubChecks();
    if (conformanceChecks.isEmpty()) {
      this.doCheck(getExchangeByUuid);
    } else {
      conformanceChecks.forEach(subCheck -> subCheck.check(getExchangeByUuid));
      if (conformanceChecks.stream().noneMatch(ConformanceCheck::isApplicable))
        this.setApplicable(false);
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

  public boolean isRelevant() {
    if (!results.isEmpty()
        && results.stream().allMatch(Predicate.not(ConformanceResult::isRelevant))) {
      return false;
    }
    return isRelevant;
  }
}
