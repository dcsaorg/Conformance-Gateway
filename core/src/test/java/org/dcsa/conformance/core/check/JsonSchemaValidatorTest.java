package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JsonSchemaValidatorTest {

  private static final Path REQUEST_NORMAL = Path.of("src/test/resources/examples/booking-request.json");
  private static final Path REQUEST_WITH_ERRORS = Path.of("src/test/resources/examples/booking-request_with_errors.json");

  @Test
  void validateJsonSchemaNormalInput() throws IOException {
    JsonSchemaValidator validator =
      JsonSchemaValidator.getInstance("/schemas/dcsaorg-DCSA_BKG-2.0.0-resolved.json", "CreateBooking");

    Set<String> validate = validateInput(validator, REQUEST_NORMAL);
    assertTrue(validate.isEmpty());
  }

  @Test
  void validateJsonWithErrorInput() throws IOException {
    JsonSchemaValidator validator =
        JsonSchemaValidator.getInstance("/schemas/dcsaorg-DCSA_BKG-2.0.0-resolved.json", "CreateBooking");

    Set<String> validate = validateInput(validator, REQUEST_WITH_ERRORS);
    assertEquals(4, validate.size());
  }

  @Test
  void validateYamlSchemaWithErrorInput() throws IOException {
    JsonSchemaValidator validator = JsonSchemaValidator.getInstance("/schemas/dcsaorg-DCSA_BKG-2.0.0-resolved.yaml", "CreateBooking");

    Set<String> validate = validateInput(validator, REQUEST_WITH_ERRORS);
    assertEquals(4, validate.size());
  }

  private static Set<String> validateInput(JsonSchemaValidator validator, Path requestPath) throws IOException {
    assertTrue(Files.exists(requestPath));
    String read = String.join("\n", Files.readAllLines(requestPath));
    return validator.validate(read);
  }
}
