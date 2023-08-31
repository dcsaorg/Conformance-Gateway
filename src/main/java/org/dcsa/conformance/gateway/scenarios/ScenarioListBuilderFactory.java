package org.dcsa.conformance.gateway.scenarios;

import org.dcsa.conformance.gateway.configuration.OrchestratorConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10ScenarioListBuilder;

public enum ScenarioListBuilderFactory {
  ; // no instances

  public static ScenarioListBuilder<?> create(
      StandardConfiguration standardConfiguration,
      OrchestratorConfiguration orchestratorConfiguration) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return EblSurrenderV10ScenarioListBuilder.buildTree(
          orchestratorConfiguration.getCarrierName(), orchestratorConfiguration.getPlatformName());
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }
}
