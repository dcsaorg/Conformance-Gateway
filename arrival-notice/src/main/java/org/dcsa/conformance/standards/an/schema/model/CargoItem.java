package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description =
        "A cargo item is the smallest unit used by stuffing. It cannot be split across containers.")
public class CargoItem {

  @Schema(
      description =
          """
The unique identifier for the equipment, which should follow the BIC ISO Container Identification Number where possible.
According to ISO 6346, a container identification code consists of a 4-letter prefix and a 7-digit number
(composed of a 3-letter owner code, a category identifier, a serial number, and a check-digit).
If a container does not comply with ISO 6346, it is suggested to follow "Recommendation #2:
Containers with non-ISO identification" from SMDG: https://smdg.org/documents/smdg-recommendations""",
      example = "APZU4812090")
  private String equipmentReference;

  @Schema(
      description =
          """
The weight of the cargo item including packaging being carried in the container.
 Excludes the tare weight of the container.""")
  private Weight cargoGrossWeight;

  @Schema(description = "Calculated by multiplying the width, height, and length of the packed cargo.")
  private Volume cargoGrossVolume;

  @Schema(
    description =
      """
The weight of the cargo item being carried in the container, excluding packaging
 and the tare weight of the container.""")
  private Weight cargoNetWeight;

  @Schema(description = "Calculated by multiplying the width, height, and length of the cargo, excluding packaging.")
  private Volume cargoNetVolume;

  @Schema(description = "Outer packaging specification")
  private OuterPackaging outerPackaging;
}
