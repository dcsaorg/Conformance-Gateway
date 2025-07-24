package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    maxLength = 50,
    example = "RAIL",
    description =
"""
Code indicating the mode of transport used for the movement of containers
between the Port of Discharge (POD) and the Place of Delivery (PDE).
""")
@AllArgsConstructor
public enum OnCarriageByCode implements EnumBase {
  RAIL("Rail"),
  TRUCK("Truck"),
  BARGE("Barge"),
  MULTIMODAL("Multimodal");

  private final String valueDescription;
}
