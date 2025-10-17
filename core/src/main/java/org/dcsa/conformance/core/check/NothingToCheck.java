package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

// SD-2358 requires non-empty list of checks
public class NothingToCheck extends ActionCheck {

  public NothingToCheck(UUID matchedExchangeUuid, HttpMessageType httpMessageType) {
    super("", "Nothing to check", (ignored) -> true, matchedExchangeUuid, httpMessageType);
  }

  @Override
  protected ConformanceCheckResult performCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    return ConformanceCheckResult.simple(Collections.emptySet());
  }
}
