package org.dcsa.conformance.specifications.an.v100.constraints;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;

@RequiredArgsConstructor
public class AttributeOneRequiresAttributeTwo implements SchemaConstraint {
  private final Field attributeFieldOne;
  private final Field attributeFieldTwo;

  @Override
  public String getDescription() {
    return "Specifying attribute '%s' requires specifying attribute '%s'."
        .formatted(attributeFieldOne.getName(), attributeFieldTwo.getName());
  }
}
