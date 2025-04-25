package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.an.v100.EnumBase;

@Schema(
    type = "string",
    example = "PRE",
    description =
"""
An indicator of whether a charge is prepaid (PRE) or collect (COL).
When prepaid, the charge is the responsibility of the shipper or the Invoice payer on behalf
of the shipper (if provided). When collect, the charge is the responsibility of the consignee
or the Invoice payer on behalf of the consignee (if provided).
""")
@AllArgsConstructor
public enum PaymentTermCode implements EnumBase {
  COL("Collect"),
  PRE("Prepaid");

  @Getter private final String valueDescription;
}
