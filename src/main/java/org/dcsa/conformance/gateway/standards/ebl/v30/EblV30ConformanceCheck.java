package org.dcsa.conformance.gateway.standards.ebl.v30;

import org.dcsa.conformance.gateway.ConformanceCheck;
import org.dcsa.conformance.gateway.ConformanceExchange;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;

import java.util.Collections;
import java.util.List;

public class EblV30ConformanceCheck extends ConformanceCheck {
  public EblV30ConformanceCheck(GatewayConfiguration gatewayConfiguration) {
    super(gatewayConfiguration);
  }

  protected List<ConformanceCheck> getSubChecks() {
    return Collections.emptyList();
  }

  @Override
  protected boolean doCheck(ConformanceExchange exchange) {
    return true; // TODO
  }
}
