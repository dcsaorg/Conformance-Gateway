package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import com.networknt.schema.oas.OpenApi30;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonSchemaValidator {
  private static final Map<String, Map<String, JsonSchemaValidator>> INSTANCES = new HashMap<>();

  private static final Map<String, Keyword> NON_VALIDATION_KEYWORDS =
      Stream.of(
              "example",
              "exclusiveMinimum",
              "openapi",
              "tags",
              "servers",
              "paths",
              "components",
              "info")
          .map(NonValidationKeyword::new)
          .collect(Collectors.toMap(NonValidationKeyword::getValue, keyword -> keyword));

  public static synchronized JsonSchemaValidator getInstance(String filePath, String schemaName) {
    return INSTANCES
        .computeIfAbsent(filePath, ignored -> new HashMap<>())
        .computeIfAbsent(schemaName, ignored -> new JsonSchemaValidator(filePath, schemaName));
  }

  private final JsonSchema jsonSchema;

  private JsonSchemaValidator(String filePath, String schemaName) {
    log.info("Loading schema: {} with schemaName: {}", filePath, schemaName);

    // Prevent warnings on unknown keywords
    OpenApi30.getInstance().getKeywords().putAll(NON_VALIDATION_KEYWORDS);

    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.getInstance(
            SpecVersion.VersionFlag.V7,
            builder ->
                builder
                    .metaSchema(OpenApi30.getInstance())
                    .defaultMetaSchemaIri(OpenApi30.getInstance().getIri())
                    .metaSchemaFactory(DisallowUnknownJsonMetaSchemaFactory.getInstance()));

    SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().typeLoose(false).build();

    jsonSchema =
        jsonSchemaFactory.getSchema(
            SchemaLocation.of("classpath:" + filePath + "#/components/schemas/" + schemaName),
            config);

    jsonSchema.initializeValidators();
  }

  @SneakyThrows
  public Set<String> validate(String jsonString) {
    try {
      return validate(OBJECT_MAPPER.readTree(jsonString));
    } catch (JsonProcessingException e) {
      String errorMessage = "Failed to parse JSON string: %s".formatted(e);
      log.info(errorMessage, e);
      return new TreeSet<>(Set.of(errorMessage));
    }
  }

  public Set<String> validate(@NonNull JsonNode jsonNode) {
    Set<ValidationMessage> validationMessageSet = jsonSchema.validate(jsonNode);
    return validationMessageSet.stream()
        .map(ValidationMessage::toString)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
