package org.dcsa.conformance.gateway.parties;

import org.dcsa.conformance.gateway.configuration.PartyConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Carrier;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Platform;

public enum ConformancePartyFactory {
  ; // no instances

  public static ConformanceParty create(
      StandardConfiguration standardConfiguration, PartyConfiguration partyConfiguration) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      if (EblSurrenderV10Role.isCarrier(partyConfiguration.getRole())) {
        return new EblSurrenderV10Carrier(partyConfiguration);
      }
      if (EblSurrenderV10Role.isPlatform(partyConfiguration.getRole())) {
        return new EblSurrenderV10Platform(partyConfiguration);
      }
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }
}
