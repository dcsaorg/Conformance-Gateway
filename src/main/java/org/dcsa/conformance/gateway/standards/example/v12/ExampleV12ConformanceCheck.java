package org.dcsa.conformance.gateway.standards.example.v12;

import java.util.Collections;
import java.util.List;
import org.dcsa.conformance.gateway.ConformanceCheck;
import org.dcsa.conformance.gateway.ConformanceExchange;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;

public class ExampleV12ConformanceCheck extends ConformanceCheck {
  public ExampleV12ConformanceCheck(GatewayConfiguration gatewayConfiguration) {
      super(gatewayConfiguration);
  }

  protected List<ConformanceCheck> getSubChecks() {
    return List.of(
        new ConformanceCheck(gatewayConfiguration) {
          @Override
          protected List<ConformanceCheck> getSubChecks() {
            return Collections.emptyList();
          }
          @Override
          protected boolean doCheck(ConformanceExchange exchange) {
            return false;
          }
        });
  }

  @Override
  protected boolean doCheck(ConformanceExchange exchange) {
    return true; // TODO
  }
}
