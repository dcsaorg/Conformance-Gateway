package org.dcsa.conformance.gateway;

import org.dcsa.conformance.gateway.standards.booking.v20.BookingV20ConformanceCheck;
import org.dcsa.conformance.gateway.configuration.GatewayConfiguration;
import org.dcsa.conformance.gateway.standards.ebl.v30.EblV30ConformanceCheck;
import org.dcsa.conformance.gateway.standards.example.v12.ExampleV12ConformanceCheck;

public class ConformanceCheckFactory {
  static ConformanceCheck create(
      GatewayConfiguration gatewayConfiguration,
      String standardName,
      String standardVersion
  ) {
    if ("Example".equals(standardName) && "1.2".equals(standardVersion)) {
      return new ExampleV12ConformanceCheck(gatewayConfiguration);
    }
    if ("Booking".equals(standardName) && "2.0".equals(standardVersion)) {
      return new BookingV20ConformanceCheck(gatewayConfiguration);
    }
    if ("EBL".equals(standardName) && "3.0".equals(standardVersion)) {
      return new EblV30ConformanceCheck(gatewayConfiguration);
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'".formatted(standardName, standardVersion));
  }
}
