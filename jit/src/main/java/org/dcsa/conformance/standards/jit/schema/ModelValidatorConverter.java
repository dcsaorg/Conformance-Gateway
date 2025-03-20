package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Iterator;

public class ModelValidatorConverter implements ModelConverter {
  @Override
  public Schema<?> resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    if (schema == null) return null;

    if (type.getType() instanceof SimpleType simpleType && simpleType.isEnumType()) {
      updateEnumDescription(simpleType.getRawClass().getName(), schema);
    }

    if (schema.getProperties() != null) {
      verifyDescriptionIsUsed(schema, type);
    }
    return schema;
  }

  private void verifyDescriptionIsUsed(Schema<?> schema, AnnotatedType type) {
    schema
        .getProperties()
        .forEach(
            (propertyName, propertySchema) -> {
              // Ignore $ref properties, since description is defined in the object.
              if (propertySchema.get$ref() != null) return;
              if (propertySchema.getDescription() == null
                  || propertySchema.getDescription().trim().isEmpty()) {
                throw new IllegalStateException(
                    String.format(
                        "Missing description for property '%s' in class: %s",
                        propertyName, type.getType().getTypeName()));
              }
            });
  }

  private void updateEnumDescription(String enumClassName, Schema<?> schema) {
    try {
      Class<?> clazz = Class.forName(enumClassName);
      Object[] enumConstants = clazz.getEnumConstants();
      if (enumConstants == null) {
        throw new IllegalStateException("Enum class has no values defined: " + enumClassName);
      }
      StringBuilder enumValues = new StringBuilder();
      for (Object enumConstant : enumConstants) {
        if (enumConstant instanceof EnumBase enumValue) {
          enumValues.append(enumValue.getDescription()).append("\n");
        } else {
          throw new IllegalStateException(
              "Enum class does not implement EnumBase: "
                  + enumClassName
                  + ". Please correct this, to ensure that a properly generated description is available.");
        }
      }
      schema.setDescription(schema.getDescription() + "\n" + enumValues);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Enum class not found: " + enumClassName);
    }
  }
}
