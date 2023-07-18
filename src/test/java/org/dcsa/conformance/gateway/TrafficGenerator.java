package org.dcsa.conformance.gateway;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TrafficGenerator extends Supplier<Stream<GeneratedTrafficExchange>> {}
