package org.dcsa.conformance.standards.an.schema;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
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
      log.info(
          "  attribute: {} {}",
          simpleType.getRawClass().getSimpleName(),
          annotatedType.getPropertyName());
      if (simpleType.isEnumType()) {
        AtomicReference<String> descriptionReference =
            new AtomicReference<>(schema.getDescription());
        Arrays.stream(annotatedType.getCtxAnnotations())
            .filter(annotation -> annotation instanceof SchemaOverride)
            .map(annotation -> (SchemaOverride) annotation)
            .filter(schemaOverride -> !schemaOverride.description().isEmpty())
            .forEach(schemaOverride -> descriptionReference.set(schemaOverride.description()));
        schema.setDescription(
            descriptionReference.get()
                + "\n"
                + Arrays.stream(
                        Class.forName(simpleType.getRawClass().getName()).getEnumConstants())
                    .map(
                        enumConstant ->
                            " - `%s` - %s"
                                .formatted(
                                    ((EnumBase) enumConstant).name(),
                                    ((EnumBase) enumConstant).getValueDescription()))
                    .collect(Collectors.joining("\n")));
      }
    } else {
      log.info("Object: {}", annotatedType.getType());
    }
    return schema;
  }
}
