package org.dcsa.conformance.standards.an.schema;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ModelValidatorConverter implements ModelConverter {
  @Override
  public Schema<?> resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    Schema<?> schema = chain.hasNext() ? chain.next().resolve(type, context, chain) : null;
    if (schema == null) return null;

    if (type.getType() instanceof SimpleType simpleType && simpleType.isEnumType()) {
      updateEnumDescription(simpleType.getRawClass().getName(), schema);
    }

    if (schema.getProperties() == null) return schema;

    schema
        .getProperties()
        .forEach(
            (propertyName, propertySchema) -> {
              processCustomAnnotations(type, propertyName, propertySchema);
              verifyDescriptionIsUsed(type, propertyName, propertySchema);
            });

    return schema;
  }

  private void verifyDescriptionIsUsed(
      AnnotatedType type, String propertyName, Schema<?> propertySchema) {
    // Ignore $ref properties, since description is defined in the object.
    if (propertySchema.get$ref() != null) return;
    if (propertySchema.getDescription() == null
        || propertySchema.getDescription().trim().isEmpty()) {
      throw new IllegalStateException(
          String.format(
              "Missing description for property '%s' in class: %s",
              propertyName, type.getType().getTypeName()));
    }
  }

  private void processCustomAnnotations(
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

    List<String> conditions = new ArrayList<>();
    if (condition.oneOf().length > 0 && !condition.oneOf()[0].isEmpty()) {
      conditions.add(
          "One of `%s` **MUST** be specified.".formatted(String.join("` or `", condition.oneOf())));
    }
    if (condition.anyOf().length > 0 && !condition.anyOf()[0].isEmpty()) {
      conditions.add(
          "At least one of `%s` **MUST** be specified. It is also acceptable to provide more than one property."
              .formatted(String.join("` or `", condition.anyOf())));
    }

    if (condition.mandatory().length > 0 && !condition.mandatory()[0].isEmpty()) {
      conditions.add(
          "Mandatory to provide if: `%s` %s provided."
              .formatted(
                  String.join("`, `", condition.mandatory()),
                  condition.mandatory().length > 1 ? "are" : "is"));
    }
    if (condition.description().length > 0 && !condition.description()[0].isEmpty()) {
      conditions.add(String.join("\n - ", condition.description()));
    }
    if (!conditions.isEmpty()) {
      if (conditions.size() == 1) {
        schema.setDescription(
            getDescription(schema) + "\n\n**Condition:** " + conditions.getFirst());
      } else {
        schema.setDescription(
            getDescription(schema) + "\n\n**Conditions:\n - " + String.join("\n - ", conditions));
      }
    }
    // Remove the first newline character, if the first line is empty. Happens with $ref properties.
    if (schema.getDescription().startsWith("\n\n")) {
      schema.setDescription(schema.getDescription().replaceFirst("\n\n", ""));
    }
    // Some descriptions are single line (non text blocks), they need 2 newlines. When there are
    // too many, remove them here.
    if (schema.getDescription().contains("\n\n\n")) {
      schema.setDescription(schema.getDescription().replace("\n\n\n", "\n\n"));
    }
  }

  private static String getDescription(Schema<?> schema) {
    return schema.getDescription() != null ? schema.getDescription() : "";
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
}
