package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "BOL",
    description = "Code indicating the type of a transport document")
@AllArgsConstructor
public enum TransportDocumentTypeCode implements EnumBase {
  BOL("Bill of Lading"),
  SWB("Sea Waybill");

  private final String valueDescription;
}
