package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "LOAD",
    description = "Code used to denote the type of an equipment event")
@AllArgsConstructor
public enum EquipmentEventTypeCode implements EnumBase {
  LOAD("Loaded"),
  DISC("Discharged"),
  GTIN("Gated in"),
  GTOT("Gated out"),
  STUF("Stuffed"),
  STRP("Stripped"),
  PICK("Pick-up"),
  DROP("Drop-off"),
  INSP("Inspected"),
  RSEA("Resealed"),
  RMVD("Removed");

  private final String valueDescription;
}
