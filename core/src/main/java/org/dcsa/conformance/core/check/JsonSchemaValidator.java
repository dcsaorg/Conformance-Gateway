package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonSchemaValidator {
  private static final Map<String, Map<String, JsonSchemaValidator>> INSTANCES = new HashMap<>();
  private static final ObjectMapper JSON_FACTORY_OBJECT_MAPPER =
      new ObjectMapper(new JsonFactory());

  public static JsonSchemaValidator getInstance(String filePath, String schemaName) {
    return INSTANCES
        .computeIfAbsent(filePath, ignored -> new ConcurrentHashMap<>())
        .computeIfAbsent(schemaName, ignored -> new JsonSchemaValidator(filePath, schemaName));
  }

  private final JsonSchema jsonSchema;

  @SneakyThrows
  private JsonSchemaValidator(String filePath, String schemaName) {
    log.info("Loading schema: {} with schemaName: {}", filePath, schemaName);
    // https://github.com/networknt/json-schema-validator/issues/579#issuecomment-1488269135

    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
            .objectMapper(JSON_FACTORY_OBJECT_MAPPER)
            .build();
    SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
    schemaValidatorsConfig.setTypeLoose(false);

    JsonSchema rootJsonSchema;
    try (InputStream schemaFileInputStream =
        JsonSchemaValidator.class.getResourceAsStream(filePath)) {

      if (schemaFileInputStream == null || schemaFileInputStream.available() == 0) {
        throw new IllegalArgumentException("Schema file not found: " + filePath);
      }
      rootJsonSchema = jsonSchemaFactory.getSchema(schemaFileInputStream, schemaValidatorsConfig);
    }

    ValidationContext validationContext =
        new ValidationContext(
            jsonSchemaFactory.getUriFactory(),
            null,
            JsonMetaSchema.getV4(),
            jsonSchemaFactory,
            schemaValidatorsConfig);
    // Prevent warnings on unknown keywords
    Map<String, Keyword> keywords = validationContext.getMetaSchema().getKeywords();
    Arrays.asList("example", "discriminator", "exclusiveMinimum")
        .forEach(keyword -> keywords.put(keyword, new NonValidationKeyword(keyword)));
    jsonSchema =
        jsonSchemaFactory.create(
            validationContext,
            "#/components/schemas/" + schemaName,
            rootJsonSchema.getSchemaNode().get("components").get("schemas").get(schemaName),
            rootJsonSchema);
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

  public Set<String> validate(JsonNode jsonNode) {
    Set<ValidationMessage> validationMessageSet = jsonSchema.validate(jsonNode);
    return validationMessageSet.stream()
        .map(ValidationMessage::toString)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
