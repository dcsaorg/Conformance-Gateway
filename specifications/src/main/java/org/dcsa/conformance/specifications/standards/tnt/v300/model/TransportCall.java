package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.CarrierServiceCode;
import org.dcsa.conformance.specifications.standards.core.v100.types.CarrierVoyageNumber;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversalServiceReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;

@Data
@Schema(description = "Transport call information")
public class TransportCall {

  @Schema(description = "Universally unique ID of the transport call.")
  private UniversallyUniqueID transportCallID;

  @Schema() private CarrierServiceCode carrierServiceCode;

  @Schema() private UniversalServiceReference universalServiceReference;

  @Schema(description = "Carrier-specific identifier of the export voyage.")
  private CarrierVoyageNumber carrierExportVoyageNumber;

  @Schema(description = "Reference of the export voyage agreed between the VSA partners.")
  private UniversalVoyageReference universalExportVoyageReference;

  @Schema(description = "Carrier-specific identifier of the import voyage.")
  private CarrierVoyageNumber carrierImportVoyageNumber;

  @Schema(description = "Reference of the import voyage agreed between the VSA partners.")
  private UniversalVoyageReference universalImportVoyageReference;
}
