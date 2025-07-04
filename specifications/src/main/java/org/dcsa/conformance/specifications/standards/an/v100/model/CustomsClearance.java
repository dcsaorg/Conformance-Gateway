package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Customs clearance information")
public class CustomsClearance {

  @Schema() private Location customsClearancePoint;

  @Schema(
      maxLength = 5,
      example = "FRMRS",
      description = "Port code used in the Carrier’s internal system to match the location")
  private String internalCode;

  @Schema() private ImmediateTransportationEntry immediateTransportationEntry;

  @Schema(
      maxLength = 1000,
      description =
"""
Instructions on the administrative processes for submitting import customs declarations to the local agency.
""")
  private String customsImportDeclarationProcedure;

  @Schema(
    maxLength = 35,
    example = "a1234",
    description =
"""
Manifest reference number for manifest declaration and customs clearance in Korea.
""")
  private String manifestReferenceNumber;

  @Schema(
    maxLength = 35,
    example = "x12345",
    description =
"""
A unique sequential number assigned by the shipping line to each Master Bill of Lading issued under a specific voyage of
a particular vessel. Customers use this Master Sequence Number (MSN) as a reference when declaring all related House B/Ls.
""")
  private String masterSequenceNumber;
}
