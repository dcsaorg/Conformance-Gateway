package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.CountryCode;
import org.dcsa.conformance.standards.an.schema.types.UniversalVoyageReference;
import org.dcsa.conformance.standards.an.schema.types.VesselIMONumber;
import org.dcsa.conformance.standards.an.schema.types.VesselVoyageDestinationTypeCode;

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
