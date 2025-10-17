package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonContentValidation {
  Set<String> validate(JsonNode body);
}
