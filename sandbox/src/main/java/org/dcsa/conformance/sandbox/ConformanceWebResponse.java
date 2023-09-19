package org.dcsa.conformance.sandbox;

import java.util.*;

public record ConformanceWebResponse(
    int statusCode,
    String contentType,
    Map<String, ? extends Collection<String>> headers,
    String body) {

  public Map<String, List<String>> getValueListHeaders() {
    HashMap<String, List<String>> stringListMap = new HashMap<>();
    for (String key : headers.keySet()) {
      stringListMap.put(key, new ArrayList<>(headers.get(key)));
    }
    return stringListMap;
  }
}
