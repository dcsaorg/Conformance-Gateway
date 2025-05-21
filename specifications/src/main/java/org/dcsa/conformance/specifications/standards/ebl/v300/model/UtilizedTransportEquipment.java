package org.dcsa.conformance.specifications.standards.ebl.v300.model;

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

  @Schema(description = "If the equipment is a Reefer Container, this flag marks it as treated like a DRY container.", example = "false")
  private Boolean isNonOperatingReefer;

  @Schema(description = "Active reefer settings applied to this equipment.")
  private ActiveReeferSettings activeReeferSettings;

  @ArraySchema(
    schema = @Schema(description = "Identifying details or markings on the packages.", example = "Made in China", maxLength = 35),
    maxItems = 50
  )
  private List<String> shippingMarks;

  @Schema(description = "List of seals applied to the equipment.")
  @ArraySchema(minItems = 1)
  private List<Seal> seals;

  @Schema(description = "A list of References associated with this equipment.")
  private List<Reference> references;

  @Schema(description = "A list of Customs references associated with this equipment.")
  private List<CustomsReference> customsReferences;
}
