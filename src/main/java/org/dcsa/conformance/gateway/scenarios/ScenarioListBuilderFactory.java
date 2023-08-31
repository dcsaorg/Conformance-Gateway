package org.dcsa.conformance.gateway.scenarios;

import java.util.Arrays;
import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.gateway.configuration.LinkConfiguration;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10ScenarioListBuilder;

public enum ScenarioListBuilderFactory {
  ; // no instances

  public static ScenarioListBuilder<?> create(ConformanceConfiguration conformanceConfiguration) {
    if ("EblSurrender".equals(conformanceConfiguration.getStandardName())
        && "1.0.0".equals(conformanceConfiguration.getStandardVersion())) {
      LinkConfiguration carrierPlatformLink =
          Arrays.stream(conformanceConfiguration.getLinks())
              .filter(link -> EblSurrenderV10Role.isCarrier(link.getSourceParty().getRole()))
              .findFirst()
              .orElseThrow();
      return EblSurrenderV10ScenarioListBuilder.buildTree(
          carrierPlatformLink.getSourceParty().getName(),
          carrierPlatformLink.getTargetParty().getName());
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(
                conformanceConfiguration.getStandardName(), conformanceConfiguration.getStandardVersion()));
  }
}
