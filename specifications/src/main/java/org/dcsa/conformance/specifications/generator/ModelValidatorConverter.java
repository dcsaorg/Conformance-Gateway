package org.dcsa.conformance.specifications.generator;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;

@Slf4j
public class ModelValidatorConverter implements ModelConverter {
  private final Map<String, Map<String, List<SchemaConstraint>>> constraintsByClassAndField;
  private final Map<String, Class<?>> modelClassesBySimpleName;
  private final Map<String, Map<String, Schema<?>>> originalSchemasByClassAndField =
      new TreeMap<>();

  public ModelValidatorConverter(
      Map<String, Map<String, List<SchemaConstraint>>> constraintsByClassAndField,
      Stream<Class<?>> modelClassesStream) {
    this.constraintsByClassAndField = constraintsByClassAndField;
    this.modelClassesBySimpleName =
        modelClassesStream.collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
  }

  @SneakyThrows
  @Override
  public Schema<?> resolve(
      AnnotatedType annotatedType,
      ModelConverterContext modelConverterContext,
      Iterator<ModelConverter> modelConverterIterator) {
    Schema<?> schema =
        modelConverterIterator.hasNext()
            ? modelConverterIterator
                .next()
                .resolve(annotatedType, modelConverterContext, modelConverterIterator)
            : null;
    if (schema == null) return null;

    if (annotatedType.getType() instanceof SimpleType simpleType) {
      log.info(
          "  attribute: {} {} {}",
          annotatedType.getParent().getName(),
          annotatedType.getPropertyName(),
          simpleType.getRawClass().getSimpleName());

      Field javaFieldWithThisPropertyName =
          getJavaFieldWithPropertyName(
              modelClassesBySimpleName.get(annotatedType.getParent().getName()),
              annotatedType.getPropertyName());
      if (javaFieldWithThisPropertyName != null
          && !java.util.List.class.isAssignableFrom(javaFieldWithThisPropertyName.getType())) {
        Arrays.stream(annotatedType.getCtxAnnotations())
            .filter(annotation -> annotation instanceof io.swagger.v3.oas.annotations.media.Schema)
            .map(annotation -> (io.swagger.v3.oas.annotations.media.Schema) annotation)
            .filter(schemaAnnotation -> !schemaAnnotation.description().isEmpty())
            .forEach(schemaAnnotation -> schema.setDescription(schemaAnnotation.description()));
      }

      originalSchemasByClassAndField
          .computeIfAbsent(annotatedType.getParent().getName(), ignoredKey -> new TreeMap<>())
          .put(annotatedType.getPropertyName(), schema);

      if (simpleType.isEnumType()) {
        schema.setDescription(
            schema.getDescription()
                + "\n"
                + Arrays.stream(
                        Class.forName(simpleType.getRawClass().getName()).getEnumConstants())
                    .map(
                        enumConstant ->
                            "- `%s` - %s"
                                .formatted(
                                    ((EnumBase) enumConstant).name(),
                                    ((EnumBase) enumConstant).getValueDescription()))
                    .collect(Collectors.joining("\n")));
      }

      constraintsByClassAndField
          .getOrDefault(annotatedType.getParent().getName(), Map.of())
          .getOrDefault(annotatedType.getPropertyName(), List.of())
          .forEach(
              schemaConstraint ->
                  schema.setDescription(
                      schema.getDescription() + "\n\n" + schemaConstraint.getDescription()));
    } else {
      log.info("Object: {}", annotatedType.getType());
      if (schema.getProperties() != null) {
        boolean clearSchemaConstraints =
            getAnnotatedTypeClass(annotatedType)
                    .getAnnotationsByType(ClearSchemaConstraints.class)
                    .length
                > 0;
        if (clearSchemaConstraints) {
          schema.required(null);
        }
        new TreeSet<>(schema.getProperties().keySet())
            .forEach(
                propertyName -> {
                  Schema<?> propertySchema = schema.getProperties().get(propertyName);
                  if (clearSchemaConstraints) {
                    propertySchema.pattern(null);
                    propertySchema.minItems(null);
                    propertySchema.minLength(null);
                  }
                  if (propertySchema.get$ref() != null) {
                    Schema<?> originalPropertySchema =
                        originalSchemasByClassAndField
                            .getOrDefault(
                                getAnnotatedTypeClass(annotatedType).getSimpleName(), Map.of())
                            .get(propertyName);
                    schema
                        .getProperties()
                        .put(
                            propertyName,
                            new ComposedSchema()
                                .allOf(List.of(new Schema<>().$ref(propertySchema.get$ref())))
                                .description(originalPropertySchema.getDescription()));
                  }
                });
      }
    }
    return schema;
  }

  private static Field getJavaFieldWithPropertyName(Class<?> aClass, String propertyName) {
    if (java.lang.Object.class.equals(aClass)) {
      return null;
    }
    return Arrays.stream(aClass.getDeclaredFields())
        .filter(field -> javaFieldHasPropertyName(field, propertyName))
        .findFirst()
        .orElse(getJavaFieldWithPropertyName(aClass.getSuperclass(), propertyName));
  }

  private static boolean javaFieldHasPropertyName(Field field, String propertyName) {
    return field.getName().equals(propertyName)
        || Arrays.stream(
                field.getAnnotationsByType(io.swagger.v3.oas.annotations.media.Schema.class))
            .anyMatch(schemaAnnotation -> schemaAnnotation.name().equals(propertyName));
  }

  @SneakyThrows
  private static Class<?> getAnnotatedTypeClass(AnnotatedType annotatedType) {
    return Class.forName(annotatedType.getType().getTypeName());
  }
}
