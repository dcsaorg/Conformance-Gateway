package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Addresses the monetary value of freight and other service charges for a `Booking`.")
@Data
public class Charge {
  @Schema(description = "Free text field describing the charge to apply", example = "Documentation fee - Destination", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String chargeName;

  @Schema(
      description =
"""
The monetary value of all freight and other service charges for a transport document, with a maximum of 2-digit decimals.
""",
      example = "1012.12",
      minimum = "0",
      format = "float")
  private Double currencyAmount;

  @Schema(
      description =
"""
The currency for the charge, using a 3-character code ([ISO 4217](https://www.iso.org/iso-4217-currency-codes.html)).
""",
      example = "DKK",
      minLength = 3,
      maxLength = 3,
      pattern = "^[A-Z]{3}$")
  private String currencyCode;

  @Schema(
      description =
"""
An indicator of whether a charge is prepaid (PRE) or collect (COL). When prepaid, the charge is the responsibility of the shipper or the Invoice payer on behalf of the shipper (if provided). When collect, the charge is the responsibility of the consignee or the Invoice payer on behalf of the consignee (if provided).

- `PRE` (Prepaid)
- `COL` (Collect)
""",
      example = "PRE",
      allowableValues = {"PRE", "COL"})
  private String paymentTermCode;

  @Schema(
      description =
"""
The code specifying the measure unit used for the corresponding unit price for this cost, such as per day, per ton, per square metre.
""",
      example = "Per day",
      maxLength = 50,
      pattern = "^\\S(?:.*\\S)?$")
  private String calculationBasis;

  @Schema(description = "The unit price of this charge item in the currency of the charge.", example = "3456.6", minimum = "0", format = "float")
  private Double unitPrice;

  @Schema(description = "The amount of unit for this charge item.", example = "34.4", minimum = "0", format = "float")
  private Double quantity;
}
