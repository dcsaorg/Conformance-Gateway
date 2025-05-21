package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Defines a list of `CargoItems` belonging together and the associated `Booking`. A `ConsignmentItem` can be split across multiple containers by referencing multiple `CargoItems`.")
@Data
public class ConsignmentItem {

  @Schema(description = "The associated booking number provided by the carrier for this `Consignment Item`.", example = "ABC709951", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String carrierBookingReference;

  @ArraySchema(schema = @Schema(description = "A line describing the cargo", example = "blue shoes size 47", maxLength = 35, pattern = "^\\S(?:.*\\S)?$"))
  private List<String> descriptionOfGoods;

  @ArraySchema(schema = @Schema(description = "Used by customs to classify the product. Must be 6 to 10 digits.", example = "851713", minLength = 6, maxLength = 10, pattern = "^\\d{6,10}$"), minItems = 1)
  private List<String> HSCodes;

  @Schema(description = "A list of National Commodity Codes applicable to this commodity.")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @ArraySchema(schema = @Schema(description = "Identifying details or markings on the packages.", example = "Made in China", maxLength = 35), maxItems = 50)
  private List<String> shippingMarks;

  @Schema(description = "List of CargoItems associated with this ConsignmentItem.")
  private List<CargoItem> cargoItems;

  @Schema(description = "Export license for the consignment item.")
  private ExportLicense exportLicense;

  @Schema(description = "Import license for the consignment item.")
  private ImportLicense importLicense;

  @Schema(description = "A list of references specific to the ConsignmentItem.")
  private List<ReferenceConsignmentItem> references;

  @Schema(description = "A list of Customs references for the ConsignmentItem.")
  private List<CustomsReference> customsReferences;
}
