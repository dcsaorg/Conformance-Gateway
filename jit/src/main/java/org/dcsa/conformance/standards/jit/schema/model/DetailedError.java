package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description = "A detailed description of what has caused the error.",
    title = "Detailed Error",
    requiredProperties = {"errorCodeText", "errorCodeMessage"})
public class DetailedError {

  @Schema(
      description =
          """
            The detailed error code returned.

             - `7000-7999` Technical error codes
             - `8000-8999` Functional error codes
             - `9000-9999` API provider-specific error codes

            [Error codes as specified by DCSA](https://developer.dcsa.org/standard-error-codes).
            """,
      example = "7003",
      minimum = "7000",
      maximum = "9999",
      format = "int32")
  private int errorCode;

  @Schema(
      description = "The property that caused the error.",
      example = "facilityCode",
      maxLength = 100)
  private String property;

  @Schema(
      description =
          "The value of the property causing the error serialised as a string exactly as in the original request.",
      example = "SG SIN WHS",
      maxLength = 500)
  private String value;

  @Schema(
      description = "A path to the property causing the error, formatted according to JSONpath.",
      example = "$.location.facilityCode",
      maxLength = 500)
  private String jsonPath;

  @Schema(
      description = "A standard short description corresponding to the errorCode.",
      maxLength = 100,
      example = "invalidData")
  private String errorCodeText;

  @Schema(
      description =
          "A long description corresponding to the `errorCode` with additional information.",
      example = "Spaces not allowed in facility code",
      maxLength = 5000)
  private String errorCodeMessage;
}
