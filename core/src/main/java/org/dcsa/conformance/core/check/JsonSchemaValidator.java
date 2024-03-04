package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class JsonSchemaValidator {
  private static final Map<String, Map<String, JsonSchemaValidator>> INSTANCES = new HashMap<>();

  public static synchronized JsonSchemaValidator getInstance(String filePath, String schemaName) {
    return INSTANCES
        .computeIfAbsent(filePath, (ignored) -> new HashMap<>())
        .computeIfAbsent(schemaName, (ignored) -> new JsonSchemaValidator(filePath, schemaName));
  }

  private final JsonSchema jsonSchema;

  @SneakyThrows
  private JsonSchemaValidator(String filePath, String schemaName) {
    // https://github.com/networknt/json-schema-validator/issues/579#issuecomment-1488269135
    InputStream schemaFileInputStream = JsonSchemaValidator.class.getResourceAsStream(filePath);
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
            .objectMapper(new ObjectMapper(new JsonFactory()))
            .build();
    SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
    schemaValidatorsConfig.setTypeLoose(false);
    JsonSchema rootJsonSchema =
        jsonSchemaFactory.getSchema(schemaFileInputStream, schemaValidatorsConfig);
    ValidationContext validationContext =
        new ValidationContext(
            jsonSchemaFactory.getUriFactory(),
            null,
            JsonMetaSchema.getV4(),
            jsonSchemaFactory,
            schemaValidatorsConfig);
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
    return validate(new ObjectMapper().readTree(jsonString));
  }

  public Set<String> validate(JsonNode jsonNode) {
    Set<ValidationMessage> validationMessageSet = jsonSchema.validate(jsonNode);
    return validationMessageSet.stream()
        .map(ValidationMessage::toString)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
