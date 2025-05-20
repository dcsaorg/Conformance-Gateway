package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Transport information for the shipment including ports, places, and planned dates.")
@Data
public class Transports {

  @Schema(description = "The planned date of arrival.", example = "2024-06-07", format = "date")
  private String plannedArrivalDate;

  @Schema(description = "The planned date of departure.", example = "2024-06-03", format = "date")
  private String plannedDepartureDate;

  @Schema(description = "Mode of transportation for pre-carriage when transport to the port of loading is organized by the carrier. The currently supported values include:\n- `VESSEL`\n- `RAIL`\n- `TRUCK`\n- `BARGE`\n- `MULTIMODAL`", example = "RAIL", maxLength = 50)
  private String preCarriageBy;

  @Schema(description = "Mode of transportation for on-carriage when transport from the port of discharge is organized by the carrier. The currently supported values include:\n- `VESSEL`\n- `RAIL`\n- `TRUCK`\n- `BARGE`\n- `MULTIMODAL`", example = "TRUCK", maxLength = 50)
  private String onCarriageBy;

  @Schema(description = "The place where the carrier takes receipt of the goods for transportation.")
  private PlaceOfReceipt placeOfReceipt;

  @Schema(description = "The port where the cargo is loaded onto the vessel.")
  private PortOfLoading portOfLoading;

  @Schema(description = "The port where the cargo is discharged from the vessel.")
  private PortOfDischarge portOfDischarge;

  @Schema(description = "The place where the carrier delivers the goods to the consignee.")
  private PlaceOfDelivery placeOfDelivery;

  @Schema(description = "Optional information about the onward inland routing.")
  private OnwardInlandRouting onwardInlandRouting;

  @Schema(description = "List of vessels/voyages relevant for the transport.")
  @ArraySchema(minItems = 1)
  private List<VesselVoyage> vesselVoyages;
}
