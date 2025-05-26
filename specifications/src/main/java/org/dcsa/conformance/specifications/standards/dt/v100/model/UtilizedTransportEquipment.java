package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Specifies the container (`equipment`), total `weight`, `volume`, possible `ActiveReeferSettings`, `seals` and `references`.")
@Data
public class UtilizedTransportEquipment {

  @Schema(description = "The container or transport equipment used.")
  private Equipment equipment;

  @Schema(description = "Indicates whether the container is shipper owned (SOC).", example = "true")
  private Boolean isShipperOwned;

  @Schema(
      description =
"""
If the equipment is a Reefer Container then setting this attribute will indicate that the container should be treated as a `DRY` container.

**Condition:** Only applicable if `ISOEquipmentCode` shows a Reefer type.
""",
      example = "false")
  private Boolean isNonOperatingReefer;

  @Schema(description = "Active reefer settings applied to this equipment.")
  private ActiveReeferSettings activeReeferSettings;

  @Schema(
      description =
"""
A list of the `ShippingMarks` applicable to this `UtilizedTransportEquipment`

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

  @Schema(description = "A list of `Seals`")
  @ArraySchema(minItems = 1)
  private List<Seal> seals;

  @Schema(description = "A list of `References`")
  private List<Reference> references;

  @Schema(description = "A list of `Customs references`")
  private List<CustomsReference> customsReferences;
}
