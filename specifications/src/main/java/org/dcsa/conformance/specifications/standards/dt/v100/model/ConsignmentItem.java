package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Defines a list of `CargoItems` belonging together and the associated `Booking`. A `ConsignmentItem` can be split across multiple containers by referencing multiple `CargoItems`.")
@Data
public class ConsignmentItem {

  @Schema(description = "The associated booking number provided by the carrier for this `Consignment Item`.", example = "ABC709951", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String carrierBookingReference;

  @Schema(
      description =
"""
A plain language description that is precise enough for Customs services to be able to identify the goods. General terms (i.e. 'consolidated', 'general cargo' 'parts' or 'freight of all kinds') or not sufficiently precise description cannot be accepted.

**Condition:** The order of the items in this array **MUST** be preserved as by the provider of the API.
""")
  @ArraySchema(
      schema =
          @Schema(
              description = "A line describing the cargo",
              example = "blue shoes size 47",
              maxLength = 35,
              pattern = "^\\S(?:.*\\S)?$"))
  private List<String> descriptionOfGoods;

  @Schema(name = "HSCodes", description = "A list of `HS Codes` that apply to this `consignmentItem`")
  @ArraySchema(schema = @Schema(description = "Used by customs to classify the product. Must be 6 to 10 digits.", example = "851713", minLength = 6, maxLength = 10, pattern = "^\\d{6,10}$"), minItems = 1)
  private List<String> hsCodes;

  @Schema(description = "A list of `National Commodity Codes` that apply to this `commodity`")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @Schema(
      description =
"""
A list of the `ShippingMarks` applicable to this `consignmentItem`

**Condition:** The order of the items in this array **MUST** be preserved as by the provider of the API.
""")
  @ArraySchema(
      schema =
          @Schema(
              description = "Identifying details or markings on the packages.",
              example = "Made in China",
              maxLength = 35),
      maxItems = 50)
  private List<String> shippingMarks;

  @Schema(description = "A list of all `cargoItems`")
  private List<CargoItem> cargoItems;

  @Schema(description = "Export license for the consignment item.")
  private ExportLicense exportLicense;

  @Schema(description = "Import license for the consignment item.")
  private ImportLicense importLicense;

  @Schema(description = "A list of `References`")
  private List<ReferenceConsignmentItem> references;

  @Schema(description = "A list of `Customs references`")
  private List<CustomsReference> customsReferences;
}
