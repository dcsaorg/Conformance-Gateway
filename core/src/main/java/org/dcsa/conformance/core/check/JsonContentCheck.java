package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public interface JsonContentCheck {
  String description();

  Set<String> validate(JsonNode body);
}
