package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Used for storing cargo during transport. Equipment size/type is defined by the ISO 6346 code. Common sizes include 20'/40'/45' containers.")
@Data
public class Equipment {

  @Schema(description = "Unique identifier for the equipment. Should follow BIC ISO 6346 identification if possible.", example = "APZU4812090", maxLength = 11, pattern = "^\\S(?:.*\\S)?$")
  private String equipmentReference;

  @Schema(description = "ISO code for the size and type of the equipment. Follows ISO 6346 standard.", example = "22G1", maxLength = 4, pattern = "^\\S(?:.*\\S)?$")
  private String ISOEquipmentCode;

  @Schema(description = "The tare weight of the container.")
  private TareWeight tareWeight;
}
