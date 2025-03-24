package org.dcsa.conformance.standards.jit.schema.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class SchemaMixin {
  @JsonIgnore public boolean exampleSetFlag; // Ignore this field during serialization
  @JsonIgnore public Object types; // Ignore the "types" field in all schemas
}
