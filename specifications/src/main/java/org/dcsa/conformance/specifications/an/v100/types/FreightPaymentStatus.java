package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.an.v100.EnumBase;

@Schema(
    type = "string",
    maxLength = 50,
    example = "FULLY_PAID",
    description =
"""
Payment status of the freight charges.
""")
@AllArgsConstructor
public enum FreightPaymentStatus implements EnumBase {
  ZERO_PAID("Zero Paid"),
  PARTIALLY_PAID("Partially Paid"),
  FULLY_PAID("Fully Paid"),
  OVERPAID("Overpaid");

  @Getter private final String valueDescription;
}
