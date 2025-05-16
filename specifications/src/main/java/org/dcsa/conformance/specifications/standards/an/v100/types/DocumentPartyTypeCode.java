package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "SHIPPER",
    description = "Code indicating the type of a document party")
@AllArgsConstructor
public enum DocumentPartyTypeCode implements EnumBase {
  SHIPPER(
"""
The party by whom or in whose name or on whose behalf a contract of carriage of goods by sea has been concluded with a
carrier, or any person by whom or in whose name, or on whose behalf, the goods are actually delivered to the carrier
in relation to the contract of carriage by sea.
"""),
  CONSIGNEE("The party to which goods are consigned."),
  FIRST_NOTIFY_PARTY("The first party to be notified of the shipment arrival."),
  SECOND_NOTIFY_PARTY("The second party to be notified of the shipment arrival."),
  OTHER_NOTIFY_PARTY("Other party to be notified of the shipment arrival.");

  private final String valueDescription;
}
