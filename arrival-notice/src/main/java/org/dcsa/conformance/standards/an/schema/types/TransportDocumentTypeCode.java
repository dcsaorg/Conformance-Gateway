package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

@Schema(
    type = "string",
    example = "BOL",
    description = "Code indicating the type of a transport document")
@AllArgsConstructor
public enum TransportDocumentTypeCode implements EnumBase {
  BOL("Bill of Lading"),
  SWB("Sea Waybill");

  @Getter
  private final String valueDescription;
}
