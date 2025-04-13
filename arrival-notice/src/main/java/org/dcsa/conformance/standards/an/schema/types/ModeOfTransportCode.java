package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

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
