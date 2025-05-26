package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "A cargoItem is the smallest unit used in stuffing. It cannot be split across containers.")
@Data
public class CargoItem {

  @Schema(
      description =
"""
The unique identifier for the equipment, which should follow the BIC ISO Container Identification Number where possible.
According to [ISO 6346](https://www.iso.org/standard/83558.html), a container identification code consists of a 4-letter prefix and a 7-digit number (composed of a 3-letter owner code, a category identifier, a serial number, and a check-digit).

If a container does not comply with [ISO 6346](https://www.iso.org/standard/83558.html), it is suggested to follow [Recommendation #2: Containers with non-ISO identification](https://smdg.org/documents/smdg-recommendations) from SMDG.
""",
      example = "APZU4812090",
      maxLength = 11,
      pattern = "^\\S(?:.*\\S)?$")
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

  @Schema(description = "A list of `National Commodity Codes` that apply to this `cargoItem`")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @Schema(description = "A list of `Customs references`")
  private List<CustomsReference> customsReferences;
}
