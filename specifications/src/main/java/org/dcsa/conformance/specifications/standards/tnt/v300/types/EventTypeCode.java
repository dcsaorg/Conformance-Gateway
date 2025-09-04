package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "EQUIPMENT",
    description = "Code used to denote the type of an event")
@AllArgsConstructor
public enum EventTypeCode implements EnumBase {
  SHIPMENT("Shipment event"),
  TRANSPORT("Transport event"),
  EQUIPMENT("Equipment event"),
  IOT("IoT event"),
  REEFER("Reefer event");

  private final String valueDescription;
}
