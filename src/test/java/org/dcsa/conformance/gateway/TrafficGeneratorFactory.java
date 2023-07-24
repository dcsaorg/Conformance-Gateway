package org.dcsa.conformance.gateway;

import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10TrafficGenerator;

public class TrafficGeneratorFactory {
  public static TrafficGenerator create(String standardName, String standardVersion) {
    if ("EblSurrender".equals(standardName) && "1.0".equals(standardVersion)) {
      return new EblSurrenderV10TrafficGenerator();
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'".formatted(standardName, standardVersion));
  }
}
