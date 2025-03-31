package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Free time condition applicable to this shipment at destination.")
public class FreeTime {

  /** "": "", "": 0, "": "WD", "": "Please see..." */
  @Schema(
      description =
          """
Code for the type of the free time. It can be one of the following codes:
 * DET (Detention)
 * DEM (Demurrage)
 * PDM (Per Diem)
 * STO (Storage)
""",
      example = "DET")
  private String typeCode;

  @Schema(
      description =
          """
Unique code for the different equipment size and type used to transport commodities.
 The code can refer to either the ISO size type (e.g. 22G1) or the ISO type group (e.g. 22GP)
 following the ISO 6346 standard.""",
      example = "22GP")
  private String isoEquipmentCode;

  @Schema(
      description =
          """
The mode of transport for which the free time object applies to as defined by DCSA.
 Can be one of the following values:
 * VESSEL (Vessel)
 * RAIL (Rail)
 * TRUCK (Truck)
 * BARGE (Barge)
""",
      example = "VESSEL")
  private String modeOfTransportCode;

  @Schema(description = "The number of units for which this free time item applies.", example = "3")
  private int quantity;

  @Schema(
      description =
          """
The unit used for the corresponding free time item, such as calendar days, working days.
 Can be one of the following values:
 * CD (Calendar Days)
 * WD (Working Days)
 * HR (Hours)
 * DOD (Day of discharge)
""",
      example = "WD")
  private String unitOfMeasure;

  @Schema(
      description =
          """
Description of the conditions under which this free time item applies.
 It can include reference to carrier website or individual charge as per service contract/agreement.""",
      example = "Calculated as...")
  private String calculationBasis;
}
