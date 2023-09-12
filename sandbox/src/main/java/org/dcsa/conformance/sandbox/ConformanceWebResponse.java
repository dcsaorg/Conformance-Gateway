package org.dcsa.conformance.sandbox;

import java.util.Collection;
import java.util.Map;

public record ConformanceWebResponse(
    int statusCode,
    String contentType,
    Map<String, ? extends Collection<String>> headers,
    String body) {}
