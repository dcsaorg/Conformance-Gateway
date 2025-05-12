package org.dcsa.conformance.specifications.an.v100;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelValidatorConverter implements ModelConverter {
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
      log.debug(
          "  attribute: {} {} {}",
          annotatedType.getParent().getName(),
          annotatedType.getPropertyName(),
          simpleType.getRawClass().getSimpleName());
      Arrays.stream(annotatedType.getCtxAnnotations())
          .filter(annotation -> annotation instanceof SchemaOverride)
          .map(annotation -> (SchemaOverride) annotation)
          .filter(schemaOverride -> !schemaOverride.description().isEmpty())
          .forEach(schemaOverride -> schema.setDescription(schemaOverride.description()));
      if (simpleType.isEnumType()) {
        schema.setDescription(
            schema.getDescription()
                + "\n"
                + Arrays.stream(
                        Class.forName(simpleType.getRawClass().getName()).getEnumConstants())
                    .map(
                        enumConstant ->
                            " * `%s` - %s"
                                .formatted(
                                    ((EnumBase) enumConstant).name(),
                                    ((EnumBase) enumConstant).getValueDescription()))
                    .collect(Collectors.joining("\n")));
      }
      AnSchemaCreator.constraintsByClassAndField
          .getOrDefault(annotatedType.getParent().getName(), Map.of())
          .getOrDefault(annotatedType.getPropertyName(), List.of())
          .forEach(
              schemaConstraint ->
                  schema.setDescription(
                      schema.getDescription() + "\n\n" + schemaConstraint.getDescription()));
    } else {
      log.debug("Object: {}", annotatedType.getType());
    }
    return schema;
  }
}
