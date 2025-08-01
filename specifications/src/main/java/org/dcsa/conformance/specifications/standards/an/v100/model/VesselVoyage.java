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

  @Schema() private VesselVoyageTypeCode typeCode;

  @Schema(maxLength = 50, example = "King of the Seas", description = "Vessel name")
  private String vesselName;

  @Schema(
      description =
"""
The flag of the nation whose laws the vessel is registered under.
This is indicated by the 2 characters for the country code using
[ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en).
""")
  private CountryCode vesselFlag;

  @Schema() private VesselIMONumber vesselIMONumber;

  @Schema(
      maxLength = 10,
      example = "NCVV",
      description =
"""
A unique alphanumeric identity that belongs to the vessel
and is assigned by the International Telecommunication Union (ITU).
It consists of a three-letter alphanumeric prefix that indicates nationality,
followed by one to four characters to identify the individual vessel.
""")
  private String vesselCallSign;

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

  @Schema(
      maxLength = 100,
      example = "CRN1234",
      description =
"""
A registration number assigned by customs to the vessel before its arrival at Port of Discharge.
It can be used for customs clearance purposes in specific countries (e.g. NL, UK).
""")
  private String callReferenceNumber;
}
