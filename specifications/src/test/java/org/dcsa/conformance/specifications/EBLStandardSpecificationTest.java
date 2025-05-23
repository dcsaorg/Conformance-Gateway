package org.dcsa.conformance.specifications;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.standards.ebl.v300.EBLStandardSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class EBLStandardSpecificationTest {
  @Test
  void testEBLStandardSpecification() {
    Map<String, Schema<?>> originalSchemas =
        SpecificationToolkit.parameterizeStringRawSchemaMap(
            new OpenAPIV3Parser()
                .read("../ebl/src/main/resources/standards/ebl/schemas/EBL_v3.0.0.yaml")
                .getComponents()
                .getSchemas());
    EBLStandardSpecification eblStandardSpecification = new EBLStandardSpecification();
    Map<String, Schema<?>> generatedSchemas =
        SpecificationToolkit.parameterizeStringRawSchemaMap(
            eblStandardSpecification.getOpenAPI().getComponents().getSchemas());
    compareType("", originalSchemas, generatedSchemas, "TransportDocument");

    eblStandardSpecification.generateArtifacts();
  }

  private static void compareType(
      String indentation,
      Map<String, Schema<?>> originalSchemasByType,
      Map<String, Schema<?>> generatedSchemasByType,
      String typeName) {
    log.debug("{}Comparing object: {}", indentation, typeName);
    Schema<?> originalTypeSchema = originalSchemasByType.get(typeName);
    Schema<?> generatedTypeSchema = generatedSchemasByType.get(typeName);

    Map<String, Schema<?>> originalProperties = getSchemaProperties(originalTypeSchema);
    Map<String, Schema<?>> generatedProperties = getSchemaProperties(generatedTypeSchema);
    softAssertEquals(
        "attribute list",
        new TreeSet<>(originalProperties.keySet()),
        new TreeSet<>(generatedProperties.keySet()));

    originalProperties.keySet().stream()
        .sorted()
        .forEach(
            attributeName -> {
              Schema<?> originalAttributeSchema = originalProperties.get(attributeName);
              Schema<?> generatedAttributeSchema = generatedProperties.get(attributeName);
              compareAttribute(
                  "  " + indentation,
                  typeName,
                  attributeName,
                  originalAttributeSchema,
                  generatedAttributeSchema);
            });

    originalProperties.keySet().stream()
        .sorted()
        .forEach(
            attributeName -> {
              Schema<?> originalAttributeSchema = originalProperties.get(attributeName);
              String attributeTypeName = getAttributeTypeName(originalAttributeSchema);
              if (attributeTypeName != null) {
                compareType(
                    "    " + indentation,
                    originalSchemasByType,
                    generatedSchemasByType,
                    attributeTypeName);
              }
            });
  }

  private static String getAttributeTypeName(Schema<?> attributeSchema) {
    if (attributeSchema.getItems() != null) {
      return getAttributeTypeName(attributeSchema.getItems());
    }
    if (attributeSchema.get$ref() != null) {
      return Arrays.stream(attributeSchema.get$ref().split("/")).toList().getLast();
    }
    return null;
  }

  private static Map<String, Schema<?>> getSchemaProperties(Schema<?> schema) {
    Map<String, Schema<?>> allProperties =
        SpecificationToolkit.parameterizeStringRawSchemaMap(schema.getProperties());
    Stream.of(schema.getAllOf(), schema.getAnyOf(), schema.getOneOf())
        .filter(Objects::nonNull)
        .forEach(
            schemaList ->
                allProperties.putAll(
                    schemaList.stream()
                        .flatMap(subSchema -> getSchemaProperties(subSchema).entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
    return allProperties;
  }

  private static void compareAttribute(
      String indentation,
      String typeName,
      String attributeName,
      Schema<?> originalAttributeSchema,
      Schema<?> generatedAttributeSchema) {
    log.debug("{}Comparing {} {}", indentation, typeName, attributeName);
    softAssertEquals(
        "name",
        comparableDescription(originalAttributeSchema.getDescription()),
        comparableDescription(generatedAttributeSchema.getDescription()));
    softAssertEquals("type", originalAttributeSchema.getType(), generatedAttributeSchema.getType());
    softAssertEquals(
        "pattern", originalAttributeSchema.getPattern(), generatedAttributeSchema.getPattern());
    softAssertEquals(
        "minLength",
        originalAttributeSchema.getMinLength(),
        generatedAttributeSchema.getMinLength());
    softAssertEquals(
        "maxLength",
        originalAttributeSchema.getMaxLength(),
        generatedAttributeSchema.getMaxLength());
    softAssertEquals(
        "examples", originalAttributeSchema.getExamples(), generatedAttributeSchema.getExamples());
  }

  private static void softAssertEquals(String property, Object expected, Object actual) {
    if (!Objects.equals(expected, actual)) {
      log.warn(
"""
WRONG VALUE:
================
{}
<<<<<<<<<<<<<<<<
{}
>>>>>>>>>>>>>>>>
{}
================
""",
          property,
          expected,
          actual);
    }
    if (System.currentTimeMillis() > 0) {
      Assertions.assertEquals(expected, actual, "Wrong value for: " + property);
    }
  }

  private static String comparableDescription(String description) {
    return description == null ? "" : description.trim();
  }
}
