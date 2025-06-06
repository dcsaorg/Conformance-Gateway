package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.dt.v100.types.DescriptionOfGoodsLine;
import org.dcsa.conformance.specifications.standards.dt.v100.types.HSCode;
import org.dcsa.conformance.specifications.standards.dt.v100.types.ShippingMark;

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
  private List<DescriptionOfGoodsLine> descriptionOfGoods;

  @Schema(name = "HSCodes", description = "A list of `HS Codes` that apply to this `consignmentItem`")
  @ArraySchema(minItems = 1)
  private List<HSCode> hsCodes;

  @Schema(description = "A list of `National Commodity Codes` that apply to this `commodity`")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @Schema(
      description =
"""
A list of the `ShippingMarks` applicable to this `consignmentItem`

**Condition:** The order of the items in this array **MUST** be preserved as by the provider of the API.
""")
  @ArraySchema(maxItems = 50)
  private List<ShippingMark> shippingMarks;

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
