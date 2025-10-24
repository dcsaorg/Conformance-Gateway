package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRebaseableContentCheck extends JsonContentCheck {
  /**
   * @param nodeToValidate The node to validate
   * @param contextPath The path to this node, which should be included in any validation errors
   *                    to describe where in the Json tree the error applies.
   * @return A set of validation errors (returns the empty set if everything is ok)
   */
  ConformanceCheckResult validate(JsonNode nodeToValidate, String contextPath);

  @Override
  default ConformanceCheckResult validate(JsonNode body) {
    return validate(body, "");
  }
}
