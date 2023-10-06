package org.dcsa.conformance.sandbox;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ConformanceWebRequest(
    String method,
    String url,
    Map<String, List<String>> queryParameters,
    Map<String, ? extends Collection<String>> headers,
    String body) {
}
