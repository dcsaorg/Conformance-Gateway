package org.dcsa.conformance.gateway;

import lombok.Getter;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public abstract class ConformanceCheck {
  @Getter private int exchangeCount = 0;
  @Getter protected ConformanceCode conformanceCode = ConformanceCode.UNKNOWN;
  protected final GatewayConfiguration gatewayConfiguration;

  public ConformanceCheck(GatewayConfiguration gatewayConfiguration) {
    this.gatewayConfiguration = gatewayConfiguration;
  }

  public boolean check(ConformanceExchange exchange) {
    ++exchangeCount;
    List<ConformanceCheck> subChecks = this.getSubChecks();
    if (subChecks.isEmpty()) {
      return this.doCheck(exchange);
    } else {
      return subChecks.stream()
              .map(subCheck -> subCheck.doCheck(exchange))
              .collect(Collectors.toList())
              .stream()
              .anyMatch(result -> result);
    }
  }

  protected abstract List<ConformanceCheck> getSubChecks();

  protected abstract boolean doCheck(ConformanceExchange exchange);
}
