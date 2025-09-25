package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "DRO",
    description = "Code used to denote the specific IoT event that has occurred")
@AllArgsConstructor
public enum IotEventCode implements EnumBase {
  DRO("Door opened"),
  LOC("Location update");

  private final String valueDescription;
}
