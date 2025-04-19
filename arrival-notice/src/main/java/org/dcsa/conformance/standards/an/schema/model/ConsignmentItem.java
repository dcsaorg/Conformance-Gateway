package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.StringPatterns;
import org.dcsa.conformance.standards.an.schema.types.CargoDescriptionLine;
import org.dcsa.conformance.standards.an.schema.types.HSCode;
import org.dcsa.conformance.standards.an.schema.types.NationalCommodityCode;
import org.dcsa.conformance.standards.an.schema.types.ShippingMark;

@Data
@Schema(
    description =
"""
A list of cargo items belonging together and the associated booking.
 A consignment item can be split across multiple containers (`UtilizedTransportEquipment`)
 by referencing multiple cargo items.""")
public class ConsignmentItem {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
      maxLength = 35,
      example = "XYZ1234",
      description = "The booking number corresponding to this consignment item")
  private String carrierBookingReference;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
      maxLength = 100,
      example = "XYZ1234",
      description =
"""
Reference to the commodity object assigned by the carrier in the booking confirmation,
 unique within the context of one booking.
""")
  private String commoditySubReference;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
"""
Ordered list of cargo descriptions precise enough for customs services to be able to identify the goods.
""")
  @ArraySchema(minItems = 1, maxItems = 150)
  private List<CargoDescriptionLine> descriptionOfGoods;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      name = "HSCodes",
      description = "List of HS Codes that apply to this consignment item")
  @ArraySchema(minItems = 1)
  private List<HSCode> hsCodes;

  @Schema(description = "The list of national commodity codes applicable to this consignment item.")
  private List<NationalCommodityCode> nationalCommodityCodes;

  @Schema(description = "Ordered list of shipping mark lines printed on the package")
  @ArraySchema(maxItems = 50)
  private List<ShippingMark> shippingMarks;

  @Schema(description = "The list of customs references applicable to this consignment item.")
  private List<CustomsReference> customsReferences;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The list of cargo items grouped in this consignment item.")
  @ArraySchema(minItems = 1)
  private List<CargoItem> cargoItems;
}
