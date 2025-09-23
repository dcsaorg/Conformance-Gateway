package org.dcsa.conformance.specifications.standards.jit.v200.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.VesselIMONumber;
import org.dcsa.conformance.specifications.standards.core.v100.types.VesselMMSINumber;

@Data
@Schema(description = "Vessel information relevant for a port call service")
public class Vessel {

  @Schema() private VesselIMONumber vesselIMONumber;

  @Schema() private VesselMMSINumber vesselMMSINumber;

  @Schema(maxLength = 50, example = "King of the Seas", description = "Vessel name")
  private String vesselName;

  @Schema(
      format = "float",
      example = "245.45",
      description =
"""
The maximum length of a ship's hull measured parallel to the waterline (Length OverAll).

If the length is specified in feet (`FOT`) then the decimal part means a fraction of a foot and **not** as a number of inches.
E.g. 120.5 feet means 120 and a half foot (which would be 120'6").
""")
  private Double lengthOverall;

  @Schema(
      format = "float",
      example = "37.33",
      description =
"""
Overall width of the ship measured at the widest point of the nominal waterline.

If the width is specified in feet (`FOT`) then the decimal part means a fraction of a foot and **not** as a number of inches.
E.g. 120.5 feet means 120 and a half foot (which would be 120'6").
""")
  private Double width;

  @Schema(
      maxLength = 3,
      example = "MTR",
      description =
"""
The unit of measure which can be expressed in:
- `MTR` (Meter)
- `FOT` (Foot)
""")
  private String dimensionUnit;

  @Schema(
      maxLength = 10,
      example = "NCVV",
      description =
"""
A unique alphanumeric identity that belongs to the vessel and is assigned by the International Telecommunication Union (ITU).
It consists of a three-letter alphanumeric prefix that indicates nationality, followed by one to four characters
to identify the individual vessel.
""")
  private String vesselCallSign;

  @Schema(
      maxLength = 4,
      example = "CONT",
      description =
"""
Categorization of ocean-going vessels distinguished by the main cargo the vessel carries:
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
  private String vesselTypeCode;
}
