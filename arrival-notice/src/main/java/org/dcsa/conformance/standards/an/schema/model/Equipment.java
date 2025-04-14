package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;

@Data
@Schema(
    description =
"""
Used for storing cargo in/on during transport. The equipment size/type is defined by the ISO 6346 code.
 The most common equipment size/type is 20'/40'/45' DRY Freight Container, but several different versions exist.
""")
public class Equipment {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 11,
      example = "APZU4812090",
      description =
"""
The unique identifier for the equipment, which should follow the BIC ISO Container Identification Number where possible.

According to [ISO 6346](https://www.iso.org/standard/83558.html), a container identification code consists of
 a 4-letter prefix and a 7-digit number (composed of a 3-letter owner code, a category identifier, a serial number,
 and a check-digit).

If a container does not comply with [ISO 6346](https://www.iso.org/standard/83558.html), it is suggested to follow
 [Recommendation #2: Containers with non-ISO identification](https://smdg.org/documents/smdg-recommendations) from SMDG.
""")
  private String equipmentReference;

  @Schema(
      name = "ISOEquipmentCode",
      maxLength = 4,
      example = "22G1",
      description =
"""
Unique code for the different equipment size and type used to transport commodities.

The code can refer to one of ISO size type (e.g. 22G1) or ISO type group (e.g. 22GP)
 following the [ISO 6346](https://www.iso.org/standard/83558.html) standard.
""")
  private String isoEquipmentCode;

  @SchemaOverride(description = "The weight of an empty container (gross container weight).")
  private Weight tareWeight;
}
