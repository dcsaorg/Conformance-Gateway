package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
  BARGE("Barge"),
  RAIL("Rail"),
  TRUCK("Truck"),
  VESSEL("Vessel");

  @Getter private final String valueDescription;
}
