package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    maxLength = 20,
    example = "VESSEL",
    description =
"""
Code defined by DCSA to identify a mode of transport.
""")
@AllArgsConstructor
public enum ModeOfTransportCode implements EnumBase {
  VESSEL("Vessel"),
  RAIL("Rail"),
  TRUCK("Truck"),
  BARGE("Barge"),
  MULTIMODAL("Multimodal");

  private final String valueDescription;
}
