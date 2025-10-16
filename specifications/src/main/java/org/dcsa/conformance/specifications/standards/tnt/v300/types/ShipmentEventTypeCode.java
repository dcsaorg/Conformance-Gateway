package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "APPR",
    description = "Code used to denote the type of a shipment event")
@AllArgsConstructor
public enum ShipmentEventTypeCode implements EnumBase {
  RECE("Received"),
  DRFT("Drafted"),
  PENA("Pending Approval"),
  PENU("Pending Update"),
  REJE("Rejected"),
  APPR("Approved"),
  ISSU("Issued"),
  SURR("Surrendered"),
  SUBM("Submitted"),
  VOID("Void"),
  CONF("Confirmed"),
  REQS("Requested"),
  CMPL("Completed"),
  HOLD("On Hold"),
  RELS("Released");

  private final String valueDescription;
}
