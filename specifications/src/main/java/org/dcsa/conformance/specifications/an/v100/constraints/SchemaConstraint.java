package org.dcsa.conformance.specifications.an.v100.constraints;

import java.lang.reflect.Field;
import java.util.List;

public interface SchemaConstraint {
  String getDescription();
  List<Field> getTargetFields();
}
