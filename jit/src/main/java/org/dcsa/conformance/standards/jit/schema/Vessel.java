package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dcsa.conformance.standards.jit.schema.enums.DimensionUnit;
import org.dcsa.conformance.standards.jit.schema.enums.VesselType;

@Data
@AllArgsConstructor
@Schema(description = "**Vessel** related to this **Port Call**.")
public class Vessel {

  @Schema(
      description =
          """
      The unique reference for a registered Vessel. The reference is the International Maritime Organisation (IMO) number, also sometimes known as the Lloyd's register code, which does not change during the lifetime of the vessel.

      **Condition:** At least one of `vesselIMONumber` or `MMSINumber` **MUST** be specified in order to identify the `Vessel`. It is also acceptable to provide both properties.
      """,
      pattern = "^\\d{7,8}$",
      minLength = 7,
      maxLength = 8)
  @JsonProperty("vesselIMONumber")
  private String vesselIMONumber;

  @Schema(
      description =
          """
      Maritime Mobile Service Identities (MMSIs) are nine-digit numbers used by maritime digital selective calling (DSC), automatic identification systems (AIS) and certain other equipment to uniquely identify a ship or a coast radio station.

      **Condition:** At least one of `vesselIMONumber` or `MMSINumber` **MUST** be specified in order to identify the `Vessel`. It is also acceptable to provide both properties.
      """,
      pattern = "^\\d{9}$",
      minLength = 9,
      maxLength = 9)
  @JsonProperty("MMSINumber")
  private String mmsiNumber;

  @Schema(
      description = "The name of the Vessel given by the Vessel Operator and registered with IMO.",
      pattern = "^\\S(?:.*\\S)?$",
      maxLength = 50)
  private String name;

  @Schema(
      description =
          """
The maximum length of a ship's hull measured parallel to the waterline (Length OverAll).

If the length is specified in feet (`FOT`) then the decimal part means a fraction of a foot and **not** as a number of inches. E.g. 120.5 feet means 120 and a half foot (which would be 120'6").
""",
      format = "float")
  private float lengthOverall;

  @Schema(
      description =
          """
      Overall width of the ship measured at the widest point of the nominal waterline.

      If the width is specified in feet (`FOT`) then the decimal part means a fraction of a foot and **not** as a number of inches. E.g. 120.5 feet means 120 and a half foot (which would be 120'6").
      """,
      format = "float",
      example = "37.33")
  private float width;

  @Schema(
      description =
          """
          The unit of measure which can be expressed in
           - `MTR` (Meter)
           - `FOT` (Foot)

          **Condition:** Mandatory to provide if `lengthOverall` or `width` is provided.
          """,
      type = "string",
      example = "MTR")
  private DimensionUnit dimensionUnit;

  @Schema(
      description =
          "A unique alphanumeric identity that belongs to the vessel and is assigned by the International Telecommunication Union (ITU). It consists of a three letter alphanumeric prefix that indicates",
      pattern = "^\\S(?:.*\\S)?$",
      maxLength = 10,
      example = "NCVV")
  private String callSign;

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
""",
      example = "CONT")
  private VesselType typeCode;
}
