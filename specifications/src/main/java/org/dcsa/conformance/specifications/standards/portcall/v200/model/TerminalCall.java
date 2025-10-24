package org.dcsa.conformance.specifications.standards.portcall.v200.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversalServiceReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;

@Data
@Schema(description = "Terminal call information")
public class TerminalCall {

  @Schema(description = "Universal unique identifier of the terminal call")
  private UniversallyUniqueID terminalCallID;

  @Schema(
      maxLength = 100,
      example = "15063401",
      description =
"""
The unique reference of a terminal call, provided by the vessel operator.

This corresponds to the property `transportCallReference` from the DCSA Operational Vessel Schedules (OVS) 3.0 standard.
""")
  private String terminalCallReference;

  @Schema(
      format = "int32",
      example = "2",
      description =
"""
Sequential number assigned to each terminal call within a port call,
indicating the order in which the terminal calls are scheduled to occur.
This number helps differentiate multiple terminal visits in the same port call.
""")
  private Integer sequenceNumber;

  @Schema(
      maxLength = 50,
      example = "Great Lion Service",
      description =
          "The carrier-specific name of the service for which the schedule details are published.")
  private String carrierServiceName;

  @Schema(
      maxLength = 11,
      example = "FE1",
      description =
          "The carrier-specific code of the service for which the schedule details are published.")
  private String carrierServiceCode;

  @Schema() private UniversalServiceReference universalServiceReference;

  @Schema(
      maxLength = 50,
      example = "2208N",
      description = "Carrier-specific identifier of the import voyage.")
  private String carrierImportVoyageNumber;

  @Schema() private UniversalVoyageReference universalImportVoyageReference;

  @Schema(
      maxLength = 50,
      example = "2208N",
      description = "Carrier-specific identifier of the export voyage.")
  private String carrierExportVoyageNumber;

  @Schema() private UniversalVoyageReference universalExportVoyageReference;

  @Schema(
      example = "false",
      description = "Flag indicating whether the terminal call is omitted by the carrier.")
  private Boolean isOmitted;
}
