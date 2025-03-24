package org.dcsa.conformance.standards.jit.schema.common;

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
}
