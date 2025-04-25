package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.CountryCode;
import org.dcsa.conformance.specifications.an.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.an.v100.types.VesselIMONumber;
import org.dcsa.conformance.specifications.an.v100.types.VesselVoyageDestinationTypeCode;

@Data
@Schema(
    description =
"""
The details of the last sea-going vessel arriving at the Port of Discharge,
which can be either a feeder or the mother vessel.
""")
public class VesselVoyage {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private VesselVoyageDestinationTypeCode typeCode;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50,
      example = "King of the Seas",
      description = "Vessel name")
  private String vesselName;

  @SchemaOverride(description = "Vessel flag")
  private CountryCode vesselFlag;

  @Schema() private VesselIMONumber vesselIMONumber;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50,
      example = "1234N",
      description =
          "The identifier of an import voyage. The carrier-specific identifier of the import Voyage.")
  private String carrierImportVoyageNumber;

  @Schema()
  private UniversalVoyageReference universalImportVoyageReference;
}
