package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public interface JsonComplexContentValidation extends JsonContentValidation{

    Set<ConformanceError> validateWithRelevance(JsonNode body);
}
