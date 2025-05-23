package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
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

  private final String valueDescription;
}
