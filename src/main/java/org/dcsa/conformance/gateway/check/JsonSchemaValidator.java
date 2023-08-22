package org.dcsa.conformance.gateway.check;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class JsonSchemaValidator {

  private final JsonSchema jsonSchema;

  public JsonSchemaValidator(String schemaFilePath, String schemaName) {
    // https://github.com/networknt/json-schema-validator/issues/579#issuecomment-1488269135
    InputStream inputStream = JsonSchemaValidator.class.getResourceAsStream(schemaFilePath);
    JsonSchemaFactory jsonSchemaFactory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
            .objectMapper(new ObjectMapper(new JsonFactory()))
            .build();
    SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
    schemaValidatorsConfig.setTypeLoose(true);
    JsonSchema rootJsonSchema = jsonSchemaFactory.getSchema(inputStream, schemaValidatorsConfig);
    ValidationContext validationContext =
        new ValidationContext(
            jsonSchemaFactory.getUriFactory(),
            null,
            JsonMetaSchema.getV7(),
            jsonSchemaFactory,
            schemaValidatorsConfig);
    jsonSchema = jsonSchemaFactory.create(
            validationContext,
            "#/components/schemas/" + schemaName,
            rootJsonSchema.getSchemaNode().get("components").get("schemas").get(schemaName),
            rootJsonSchema);
    jsonSchema.initializeValidators();
  }

  @SneakyThrows
  public Set<String> validate(String jsonString) {
    Set<ValidationMessage> validationMessageSet =
        jsonSchema.validate(new ObjectMapper().readTree(jsonString));
    return validationMessageSet.stream()
        .map(ValidationMessage::toString)
        .collect(Collectors.toSet());
  }
}