package org.dcsa.conformance.core.state;

import com.fasterxml.jackson.databind.JsonNode;

public interface StatefulEntity {
  JsonNode exportJsonState();

  void importJsonState(JsonNode jsonState);
}
