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

    processCustomAnnotations(schema, type);

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

  private void processCustomAnnotations(Schema<?> schema, AnnotatedType type) {
    if (schema.getProperties() != null) {
      schema
          .getProperties()
          .forEach(
              (propertyName, propertySchema) ->
                  processFoundSchemaObjects(type, propertyName, propertySchema));
    }
  }

  private void processFoundSchemaObjects(
      AnnotatedType type, String propertyName, Schema<?> propertySchema) {
    try {
      // Some classes are of type SimpleType, and we need to extract the class name differently.
      String className;
      if (type.getType() instanceof SimpleType simpleType) {
        className = simpleType.getRawClass().getName();
      } else className = type.getType().getTypeName();
      Class<?> clazz = Class.forName(className);
      Field field = getFieldFromClass(clazz, propertyName);

      processSchemaOverride(propertySchema, field);
      processConditionAnnotations(propertySchema, field);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "While processing property '%s' in '%s': %s"
              .formatted(propertyName, type.getType().getTypeName(), e));
    }
  }

  private static void processSchemaOverride(Schema<?> propertySchema, Field field) {
    SchemaOverride override = field.getAnnotation(SchemaOverride.class);
    if (override == null) return;
    if (!override.description().isEmpty()) {
      propertySchema.setDescription(override.description());
    }
    if (!override.example().isEmpty()) {
      propertySchema.setExample(override.example());
    }
  }

  private void processConditionAnnotations(Schema<?> schema, Field field) {
    Condition condition = field.getAnnotation(Condition.class);
    if (condition == null) return;

    if (!condition.required().isEmpty()) {
      schema.setDescription(
          schema.getDescription()
              + "%n**Condition:** Mandatory if `%s` is provided.".formatted(condition.required()));
    }

    if (condition.oneOf().length > 0 && !condition.oneOf()[0].isEmpty()) {
      schema.setDescription(
          schema.getDescription()
              + "%n**Condition:** One of `%s` **MUST** be specified."
                  .formatted(String.join("` or `", condition.oneOf())));
    }
    if (condition.anyOf().length > 0 && !condition.anyOf()[0].isEmpty()) {
      schema.setDescription(
          schema.getDescription()
              + "%n**Condition:** At least one of `%s` **MUST** be specified. It is also acceptable to provide more than one property."
                  .formatted(String.join("` or `", condition.anyOf())));
    }

    if (condition.allOf().length > 0 && !condition.allOf()[0].isEmpty()) {
      schema.setDescription(
          schema.getDescription()
              + "%n**Condition:** All of the following properties **MUST** be specified: `%s`."
                  .formatted(String.join("` and `", condition.allOf())));
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
