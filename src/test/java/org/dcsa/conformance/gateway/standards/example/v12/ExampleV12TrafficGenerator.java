package org.dcsa.conformance.gateway.standards.example.v12;

import org.dcsa.conformance.gateway.GeneratedTrafficExchange;
import org.dcsa.conformance.gateway.TrafficGenerator;

import java.util.stream.Stream;

public class ExampleV12TrafficGenerator implements TrafficGenerator {
  @Override
  public Stream<GeneratedTrafficExchange> get() {
    return Stream.of(
            new GeneratedTrafficExchange(
                "/v3/service-schedules?carrierServiceCode=AAA",
                "{\"RequestBodyKey\": \"RequestBodyValue\"}",
                "{\"MockResponseFor\": \"carrierServiceCode=AAA\"}"),
            new GeneratedTrafficExchange(
                "/v3/service-schedules?vesselIMONumber=1111111",
                "{\"RequestBodyKey\": \"RequestBodyValue\"}",
                "{\"MockResponseFor\": \"vesselIMONumber=1111111\"}"))
        .limit(111);
  }
}
