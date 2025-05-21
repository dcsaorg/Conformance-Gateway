package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "A cargoItem is the smallest unit used in stuffing. It cannot be split across containers.")
@Data
public class CargoItem {

  @Schema(description = "Unique identifier for the equipment (e.g., container). Preferably follows ISO 6346 format.", example = "APZU4812090", maxLength = 11, pattern = "^\\S(?:.*\\S)?$")
  private String equipmentReference;

  @Schema(description = "Gross weight of the cargo.")
  private CargoGrossWeight cargoGrossWeight;

  @Schema(description = "Gross volume of the cargo.")
  private CargoGrossVolume cargoGrossVolume;

  @Schema(description = "Net weight of the cargo.")
  private CargoNetWeight cargoNetWeight;

  @Schema(description = "Net volume of the cargo.")
  private CargoNetVolume cargoNetVolume;

  @Schema(description = "Export license applicable to this cargo item.")
  private ExportLicense exportLicense;

  @Schema(description = "Import license applicable to this cargo item.")
  private ImportLicense importLicense;

  @Schema(description = "Packaging information for this cargo item.")
  private OuterPackaging outerPackaging;

  @Schema(description = "List of national commodity codes applicable to this cargo item.")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @Schema(description = "List of customs references associated with this cargo item.")
  private List<CustomsReference> customsReferences;
}
