package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "PRE",
    description = "Code used to denote the type of a shipment location")
@AllArgsConstructor
public enum ShipmentLocationTypeCode implements EnumBase {
  PRE("Place of Receipt"),
  POL("Port of Loading"),
  POD("Port of Discharge"),
  PDE("Place of Delivery"),
  PCF("Pre-carriage From"),
  OIR("Onward Inland Routing"),
  ORI("Origin of goods"),
  IEL("Container intermediate export stop-off location"),
  PTP("Prohibited transshipment port"),
  RTP("Requested transshipment port"),
  FCD("Full container drop-off location");

  private final String valueDescription;
}
