package org.dcsa.conformance.standards.jit.schema.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DCSABase {

  public static final Map<String, Header> API_VERSION_HEADER =
      Map.of("API-Version", new Header().$ref("#/components/headers/API-Version"));
  public static final String JSON_CONTENT_TYPE = "application/json";

  public static Schema<String> getErrorResponseSchema() {
    return new Schema<>().$ref("#/components/schemas/ErrorResponse");
  }

  public static Contact getDefaultContact() {
    return new Contact()
        .name("Digital Container Shipping Association (DCSA)")
        .url("https://dcsa.org")
        .email("info@dcsa.org");
  }

  public static License getDefaultLicense() {
    return new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html");
  }

  public static String readFileFromResources(String fileName) {
    try {
      return Files.readString(
          Paths.get(DCSABase.class.getClassLoader().getResource(fileName).toURI()));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to read file from resources: " + fileName, e);
    }
  }

  public static void generateYamlFile(OpenAPI openAPI, String yamlFilePath) throws IOException {
    // Export to YAML
    YAMLFactory yamlFactory =
        YAMLFactory.builder()
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .configure(YAMLGenerator.Feature.SPLIT_LINES, false)
            .build();

    ObjectMapper mapper = new ObjectMapper(yamlFactory);
    mapper.findAndRegisterModules();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
    // Prevent date-time example values getting converted to unix timestamps.
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.addMixIn(Schema.class, SchemaMixin.class);
    mapper.addMixIn(Object.class, ValueSetFlagIgnoreMixin.class); // Remove valueSetFlag attribute.

    String yamlContent = mapper.writeValueAsString(openAPI);
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    System.out.printf("OpenAPI spec exported to %s%n", yamlFilePath);
  }
}
