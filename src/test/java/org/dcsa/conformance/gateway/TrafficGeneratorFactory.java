package org.dcsa.conformance.gateway;

import org.dcsa.conformance.gateway.standards.example.v12.ExampleV12TrafficGenerator;

public class TrafficGeneratorFactory {
    public static TrafficGenerator create(
            String standardName,
            String standardVersion
    ) {
        if ("Example".equals(standardName) && "1.2".equals(standardVersion)) {
            return new ExampleV12TrafficGenerator();
        }
        throw new UnsupportedOperationException(
                "Unsupported standard '%s' version '%s'".formatted(standardName, standardVersion));
    }
}
