package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.types.CountryCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.VesselIMONumber;
import org.dcsa.conformance.specifications.standards.an.v100.types.VesselVoyageTypeCode;

@Data
@Schema(
    description =
"""
Details of a vessel voyage.

When `typeCode` is `POL` (port of loading), these are the details of
the first sea going vessel where the container was initially loaded.

When `typeCode` is `POD` (port of destination), these are the details of
the last sea-going vessel arriving at the Port of Discharge,
which can be either a feeder or the mother vessel.

When `typeCode` is `DC` (destination country), these are the details of
the first mother vessel arriving at the destination country.
""")
public class VesselVoyage {

  @Schema()
  private VesselVoyageTypeCode typeCode;

  @Schema(
      maxLength = 50,
      example = "King of the Seas",
      description = "Vessel name")
  private String vesselName;

  @Schema(description = "Vessel flag")
  private CountryCode vesselFlag;

  @Schema() private VesselIMONumber vesselIMONumber;

  @Schema(
      maxLength = 50,
      example = "1234N",
      description =
"""
Carrier-specific identifier of a voyage:
* the import voyage (if `typeCode` is `POD` or `DC`)
* the export voyage (if `typeCode` is `POL`)
""")
  private String carrierVoyageNumber;

  @Schema(
      description =
"""
Universal identifier of a voyage:
* the import voyage (if `typeCode` is `POD` or `DC`)
* the export voyage (if `typeCode` is `POL`)
""")
  private UniversalVoyageReference universalVoyageReference;
}
