package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.DocumentTypeCode;

@Data
@Schema(description = "Referenced document")
public class DocumentReference {

  @Schema() private DocumentTypeCode typeCode;

  @Schema(
      description = "Reference identifying the document",
      example = "123e4567e89b",
      maxLength = 100)
  private String reference;
}
