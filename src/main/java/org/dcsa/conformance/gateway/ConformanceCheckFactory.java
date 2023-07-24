package org.dcsa.conformance.gateway;

import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10ConformanceCheck;

public class ConformanceCheckFactory {
  static ConformanceCheck create(
      String standardName,
      String standardVersion) {
    if ("EblSurrender".equals(standardName) && "1.0".equals(standardVersion)) {
      return new EblSurrenderV10ConformanceCheck();
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'".formatted(standardName, standardVersion));
  }
}
