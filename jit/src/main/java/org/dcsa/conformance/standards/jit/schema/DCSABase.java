package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;

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
}
