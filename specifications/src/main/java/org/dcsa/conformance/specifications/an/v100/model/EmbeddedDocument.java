package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Name, Base64-encoded content and content type of an embedded document")
public class EmbeddedDocument {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      example = "20250123 AN XYZ01234.docx",
      description = "Document name")
  private String name;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      example = "RXhhbXBsZSBBTiBjb250ZW50",
      description = "Base64 representation of the binary document content")
  private String content;

  @Schema(
      type = "string",
      defaultValue = "application/pdf",
      example = "application/msword",
      description = "Content type of document")
  private String contentType;
}
