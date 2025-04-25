package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.an.v100.EnumBase;

@Schema(
    type = "string",
    example = "POD",
    description =
"""
Code representing the destination type of a vessel voyage.
""")
@AllArgsConstructor
public enum VesselVoyageDestinationTypeCode implements EnumBase {
  DC("destination country"),
  POD("port of destination");

  @Getter
  private final String valueDescription;
}
