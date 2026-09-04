package org.dcsa.conformance.standards.vgm.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.KeywordDataset;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class VgmDataSets {

  private static final String SCHEMA_RESOURCE = "/standards/vgm/schemas/VGM_v1.0.0.yaml";
  private static final Pattern BACKTICKED_CODE_PATTERN = Pattern.compile("`([^`]+)`");
  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  private static final Set<String> DEFAULT_VGM_WEIGHT_UNIT = Set.of("KGM", "LBR");
  private static final Set<String> DEFAULT_VGM_METHOD = Set.of("SM1", "SM2");

  public static final KeywordDataset VGM_WEIGHT_UNIT =
      loadDatasetFromSchemaDescription(
          DEFAULT_VGM_WEIGHT_UNIT,
          "components",
          "schemas",
          "Weight",
          "properties",
          "unit");

  public static final KeywordDataset VGM_METHOD =
      loadDatasetFromSchemaDescription(
          DEFAULT_VGM_METHOD,
          "components",
          "schemas",
          "VGM",
          "properties",
          "method");

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
    try (InputStream inputStream = VgmDataSets.class.getResourceAsStream(SCHEMA_RESOURCE)) {
      if (inputStream == null) {
        log.warn("Could not load VGM schema resource at {}. Falling back to built-in datasets.", SCHEMA_RESOURCE);
        return YAML_MAPPER.createObjectNode();
      }
      return YAML_MAPPER.readTree(inputStream);
    } catch (IOException e) {
      log.warn("Failed parsing VGM schema resource {}. Falling back to built-in datasets.", SCHEMA_RESOURCE, e);
      return YAML_MAPPER.createObjectNode();
    }
  }
}
