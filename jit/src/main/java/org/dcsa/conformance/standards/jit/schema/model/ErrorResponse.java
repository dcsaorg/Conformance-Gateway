package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    description = "Unexpected error.",
    title = "Error Response",
    requiredProperties = {
      "httpMethod",
      "requestUri",
      "statusCode",
      "statusCodeText",
      "errorDateTime",
      "errors"
    })
public class ErrorResponse {

  @Schema(
      description = "The HTTP method used to make the request e.g. `GET`, `POST`, etc.",
      example = "POST",
      allowableValues = {"GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"})
  private String httpMethod;

  @Schema(
      description = "The URI that was requested.",
      example = "/port-call-services/0342254a-5927-4856-b9c9-aa12e7c00563")
  private String requestUri;

  @Schema(description = "The HTTP status code returned.", format = "int32", example = "400")
  private Integer statusCode;

  @Schema(
      description = "A standard short description corresponding to the HTTP status code.",
      example = "Bad Request",
      maxLength = 50)
  private String statusCodeText;

  @Schema(
      description =
          "A long description corresponding to the HTTP status code with additional information.",
      example = "The supplied data could not be accepted",
      maxLength = 200)
  private String statusCodeMessage;

  @Schema(
      description = "A unique identifier to the HTTP request within the scope of the API provider.",
      maxLength = 100,
      example = "4426d965-0dd8-4005-8c63-dc68b01c4962")
  private String providerCorrelationReference;

  @Schema(
      description = "The DateTime corresponding to the error occurring.",
      example = "2024-09-04T09:41:00Z",
      format = "date-time")
  private String errorDateTime;

  @Schema(
      description = "An array of errors providing more detail about the root cause.",
      minLength = 1,
      type = "array",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private DetailedError[] errors;
}
