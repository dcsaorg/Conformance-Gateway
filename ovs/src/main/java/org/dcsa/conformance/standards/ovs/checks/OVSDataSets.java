package org.dcsa.conformance.standards.ovs.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.KeywordDataset;

@Slf4j
public class OVSDataSets {

  private static final String SCHEMA_RESOURCE = "/standards/ovs/schemas/OVS_v3.0.0.yaml";
  private static final Pattern BACKTICKED_CODE_PATTERN = Pattern.compile("`([^`]+)`");
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  private static final Set<String> DEFAULT_STATUS_CODE =
      Set.of("OMIT", "BLNK", "ADHO", "PHOT", "PHIN", "SLID", "ROTC", "CUTR");
  private static final Set<String> DEFAULT_STATUS_CODES =
      Set.of("OMIT", "BLNK", "ADHO", "PHOT", "PHIN", "SLID", "ROTC", "CUTR", "DRYD", "BUNK", "OOSV");

  public static final KeywordDataset STATUS_CODE =
      loadDatasetFromSchemaDescription(
          DEFAULT_STATUS_CODE,
          "components",
          "schemas",
          "TransportCall",
          "properties",
          "statusCode");

  public static final KeywordDataset STATUS_CODES =
      loadDatasetFromSchemaDescription(
          DEFAULT_STATUS_CODES,
          "components",
          "schemas",
          "TransportCall",
          "properties",
          "statusCodes",
          "items");

  private static KeywordDataset loadDatasetFromSchemaDescription(
      Set<String> fallbackValues, String... pathSegments) {
    Set<String> extractedValues = extractFromSchemaDescription(pathSegments);
    Set<String> resolvedValues = extractedValues.isEmpty() ? fallbackValues : extractedValues;
    return KeywordDataset.staticDataset(resolvedValues.toArray(String[]::new));
  }

  private static Set<String> extractFromSchemaDescription(String... pathSegments) {
    JsonNode schemaRoot = loadSchemaRoot();
    JsonNode node = schemaRoot;
    for (String pathSegment : pathSegments) {
      node = node.path(pathSegment);
    }
    String description = node.path("description").asText("");
    LinkedHashSet<String> values = new LinkedHashSet<>();
    Matcher matcher = BACKTICKED_CODE_PATTERN.matcher(description);
    while (matcher.find()) {
      values.add(matcher.group(1));
    }
    return values;
  }

  private static JsonNode loadSchemaRoot() {
    try (InputStream inputStream = OVSDataSets.class.getResourceAsStream(SCHEMA_RESOURCE)) {
      if (inputStream == null) {
        log.warn("Could not load OVS schema resource at {}. Falling back to built-in datasets.", SCHEMA_RESOURCE);
        return YAML_MAPPER.createObjectNode();
      }
      return YAML_MAPPER.readTree(inputStream);
    } catch (IOException e) {
      log.warn("Failed parsing OVS schema resource {}. Falling back to built-in datasets.", SCHEMA_RESOURCE, e);
      return YAML_MAPPER.createObjectNode();
    }
  }
}
