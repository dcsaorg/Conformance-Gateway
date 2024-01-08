package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;

public interface MultiAttributeValidator {

  AttributePathBuilder at(JsonPointer pointer);
  AttributePathBuilder path(String path);

  default MultiAttributeValidator submitAllMatching(String pseudoPath) {
    String[] parts = pseudoPath.split("\\.");
    if (parts[0].startsWith(".") || parts[0].startsWith("$") || parts[0].startsWith("/")) {
      throw new IllegalArgumentException("Invalid path " + pseudoPath + ": Please start with `attribute.<...>`");
    }
    if (parts[0].equals("*")) {
      throw new IllegalArgumentException("Invalid path " + pseudoPath + ": Cannot start on a wildcard/all");
    }
    if (parts[0].contains("*")) {
      throw new IllegalArgumentException("Invalid path " + pseudoPath + ": Segments cannot contain wildcards (a*.b is not supported)");
    }
    var apb = this.path(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      var segment = parts[i];
      if (segment.equals("*")) {
        apb = apb.all();
      } else if (segment.contains("*")) {
        throw new IllegalArgumentException("Invalid path " + pseudoPath + ": Segments cannot contain wildcards (a.foo*.c is not supported)");
      } else {
        apb = apb.path(segment);
      }
    }
    return apb.submitPath();
  }

  interface AttributePathBuilder {
    AttributePathBuilder all();
    AttributePathBuilder at(JsonPointer pointer);
    AttributePathBuilder path(String path);

    MultiAttributeValidator submitPath();
  }
}
