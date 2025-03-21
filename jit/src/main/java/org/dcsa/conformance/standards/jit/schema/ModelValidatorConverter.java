package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class ModelValidatorConverter implements ModelConverter {
  @Override
  public Schema<?> resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    if (schema == null) return null;

    processDescriptionOverride(schema, type);

    if (type.getType() instanceof SimpleType simpleType && simpleType.isEnumType()) {
      updateEnumDescription(simpleType.getRawClass().getName(), schema);
    }

    // TODO: updateConditionAnnotations(schema, type); required : {1,2,3}

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

  private void processDescriptionOverride(Schema<?> schema, AnnotatedType type) {
    if (schema.getProperties() != null) {
      schema
          .getProperties()
          .forEach(
              (propertyName, propertySchema) -> {
                try {
                  Class<?> clazz = Class.forName(type.getType().getTypeName());
                  java.lang.reflect.Field field = getFieldFromClass(clazz, propertyName);
                  SchemaOverride override = field.getAnnotation(SchemaOverride.class);
                  if (override != null && !override.description().isEmpty()) {
                    propertySchema.setDescription(override.description());
                  }
                  if (override != null && !override.example().isEmpty()) {
                    propertySchema.setExample(override.example());
                  }
                } catch (ClassNotFoundException e) {
                  throw new IllegalStateException(
                      "While processing property '%s' in '%s': %s"
                          .formatted(propertyName, type.getType().getTypeName(), e));
                }
              });
    }
  }

  private Field getFieldFromClass(Class<?> clazz, String propertyName) {
    try {
      return clazz.getDeclaredField(propertyName);
    } catch (NoSuchFieldException e) {
      // Field might be renamed by 'name' property in @Schema annotation.
    }
    // Continue to search for all fields in the given class, and match the 'name' property with an
    // existing Field (Java class property).
    AtomicReference<String> foundPropertyName = new AtomicReference<>();
    Arrays.stream(clazz.getDeclaredFields())
        .forEach(
            field -> {
              if (field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class) != null) {
                io.swagger.v3.oas.annotations.media.Schema schema =
                    field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
                if (schema.name().equals(propertyName)) {
                  foundPropertyName.set(field.getName());
                }
              }
            });
    if (foundPropertyName.get() != null) {
      try {
        return clazz.getDeclaredField(foundPropertyName.get());
      } catch (NoSuchFieldException e) {
        throw new IllegalStateException(e);
      }
    }
    throw new IllegalStateException("Field not found: " + propertyName);
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
