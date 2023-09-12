package org.dcsa.conformance.gateway;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record ConformanceWebRequest(
    String method,
    String url,
    String uri,
    Map<String, List<String>> queryParameters,
    Map<String, ? extends Collection<String>> headers,
    String body) {

  public String baseUrl() {
    return url.substring(0, url.length() - uri.length());
  }
}
