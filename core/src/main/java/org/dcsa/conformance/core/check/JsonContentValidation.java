package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;

public interface JsonContentValidation {
  Set<String> validate(JsonNode body);
}
