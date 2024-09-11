package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JsonSchemaValidatorTest {

  @Test
  void validateJsonSchemaValidator() throws IOException {
    JsonSchemaValidator validator =
        JsonSchemaValidator.getInstance("/schemas/booking-api-v20.json", "CreateBooking");

    Path path = Paths.get("src/test/resources/examples/booking-request.json");
    assertTrue(Files.exists(path));
    String read = String.join("\n", Files.readAllLines(path));
    Set<String> validate = validator.validate(read);
    assertTrue(validate.isEmpty());
  }
}
