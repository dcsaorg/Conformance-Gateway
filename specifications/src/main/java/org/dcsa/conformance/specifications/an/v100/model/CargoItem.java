package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.EquipmentReference;

@Data
@Schema(
    description =
        "A cargo item is the smallest unit used by stuffing. It cannot be split across containers.")
public class CargoItem {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private EquipmentReference equipmentReference;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
Weight of the cargo item, including the packaging but excluding the container tare weight.
""")
  private Weight cargoGrossWeight;

  @SchemaOverride(
      description =
"""
Volume calculated by multiplying the width, height, and length of the packed cargo, including packaging.
""")
  private Volume cargoGrossVolume;

  @SchemaOverride(
      description =
"""
Weight of the cargo item, excluding both the packaging and the container tare weight.
""")
  private Weight cargoNetWeight;

  @SchemaOverride(
      description =
"""
Volume calculated by multiplying the width, height, and length of the packed cargo, excluding packaging.
""")
  private Volume cargoNetVolume;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private OuterPackaging outerPackaging;
}
