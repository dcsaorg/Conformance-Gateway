package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public interface JsonRebaseableContentCheck extends JsonContentCheck {
  /**
   * @param nodeToValidate The node to validate
   * @param contextPath The path to this node, which should be included in any validation errors
   *                    to describe where in the Json tree the error applies.
   * @return A set of validation errors (returns the empty set if everything is ok)
   */
  Set<String> validate(JsonNode nodeToValidate, String contextPath);

  @Override
  default Set<String> validate(JsonNode body) {
    return validate(body, "");
  }
}
