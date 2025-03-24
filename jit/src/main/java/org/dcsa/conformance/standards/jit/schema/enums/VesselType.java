package org.dcsa.conformance.standards.jit.schema.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.common.EnumBase;

@Schema(
    description =
        "Categorization of ocean-going vessels distinguished by the main cargo the vessel carries. Possible values:")
@AllArgsConstructor
public enum VesselType implements EnumBase {
  GCGO("General cargo"),
  CONT("Container"),
  RORO("RoRo"),
  CARC("Car carrier"),
  PASS("Passenger"),
  FERY("Ferry"),
  BULK("Bulk"),
  TANK("Tanker"),
  LGTK("Liquefied gas tanker"),
  ASSI("Assistance"),
  PILO("Pilot boat");

  private final String fullName;

  public String getDescription() {
    return " - `%s` (%s)".formatted(name(), fullName);
  }
}
