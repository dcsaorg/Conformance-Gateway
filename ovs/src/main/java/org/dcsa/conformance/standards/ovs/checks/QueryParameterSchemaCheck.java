package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class QueryParameterSchemaCheck extends ActionCheck {

  private final JsonNode parametersSchema; // Store the "parameters" array from OpenAPI spec
  private final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

  public QueryParameterSchemaCheck(
    String titlePrefix,
    String description,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    String schemaPath) {
    super(titlePrefix, description, isRelevantForRoleName, matchedExchangeUuid, HttpMessageType.REQUEST);
    try {
      this.parametersSchema = loadQueryParametersSchema(schemaPath, "/v3/service-schedules");
    } catch (IOException e) {
      throw new RuntimeException("Failed to load JSON schema", e);
    }
  }

  private JsonNode loadQueryParametersSchema(String openApiSpecPath, String endpointPath) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream inputStream = getClass().getResourceAsStream(openApiSpecPath);
    JsonNode openApiSpec = objectMapper.readTree(inputStream);

    // Get the "parameters" array, which contains schemas for individual parameters
    return openApiSpec.path("paths")
      .path(endpointPath)
      .path("get")
      .path("parameters");
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    Set<String> errors = new HashSet<>();
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) {
      return Set.of();
    }
    Map<String, String> queryParams = exchange.getRequest().queryParams()
      .entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().iterator().next()));

    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
      String paramName = entry.getKey();
      String paramValue = entry.getValue();

      // Find the schema for the parameter by name
      JsonNode paramSchema = null;
      Iterator<JsonNode> elements = parametersSchema.elements();
      while (elements.hasNext()) {
        JsonNode p = elements.next();
        if (p.path("name").asText().equals(paramName)) {
          paramSchema = p.path("schema");
          break;
        }
      }

      if (paramSchema != null) {
        // Convert the parameter value to a JsonNode, handling special case for "limit" parameter
        JsonNode paramValueNode;
        if ("limit".equals(paramName)) {
          paramValueNode = OBJECT_MAPPER.valueToTree(Integer.parseInt(paramValue));
        } else {
          paramValueNode = OBJECT_MAPPER.valueToTree(paramValue);
        }

        // Validate the parameter value against its schema
        JsonSchema jsonSchema = factory.getSchema(paramSchema);
        Set<ValidationMessage> validationResult = jsonSchema.validate(paramValueNode);
        errors.addAll(validationResult.stream()
          .map(ValidationMessage::getMessage)
          .collect(Collectors.toSet()));
      }
    }
    return errors;
  }
}
