package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonContentValidation {
  ConformanceCheckResult validate(JsonNode body);
}
