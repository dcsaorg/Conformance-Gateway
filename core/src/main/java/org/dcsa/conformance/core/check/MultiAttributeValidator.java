package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonPointer;

public interface MultiAttributeValidator {

  AttributePathBuilder at(JsonPointer pointer);
  AttributePathBuilder path(String path);

  interface AttributePathBuilder {
    AttributePathBuilder all();
    AttributePathBuilder at(JsonPointer pointer);
    AttributePathBuilder path(String path);

    MultiAttributeValidator submitPath();
  }
}
