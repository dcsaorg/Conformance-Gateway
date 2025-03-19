package org.dcsa.conformance.standards.jit.schema.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(
    description =
        """
Categorization of ocean-going vessels distinguished by the main cargo the vessel carries. Possible values:
- `GCGO` (General cargo)
- `CONT` (Container)
- `RORO` (RoRo)
- `CARC` (Car carrier)
- `PASS` (Passenger)
- `FERY` (Ferry)
- `BULK` (Bulk)
- `TANK` (Tanker)
- `LGTK` (Liquefied gas tanker)
- `ASSI` (Assistance)
- `PILO` (Pilot boat)
""")
public enum VesselType {
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
}
