package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Addresses the monetary value of freight and other service charges for a `Booking`.")
@Data
public class Charge {
  @Schema(description = "Free text field describing the charge to apply", example = "Documentation fee - Destination", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String chargeName;

  @Schema(description = "The monetary value of all freight and other service charges for a transport document.", example = "1012.12", minimum = "0", format = "float")
  private Double currencyAmount;

  @Schema(description = "The currency for the charge, using a 3-character ISO 4217 code.", example = "DKK", minLength = 3, maxLength = 3, pattern = "^[A-Z]{3}$")
  private String currencyCode;

  @Schema(description = "Whether the charge is prepaid (PRE) or collect (COL).", example = "PRE", allowableValues = {"PRE", "COL"})
  private String paymentTermCode;

  @Schema(description = "Unit of measurement for unit price, such as per day, per ton.", example = "Per day", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String calculationBasis;

  @Schema(description = "The unit price of this charge item.", example = "3456.6", minimum = "0", format = "float")
  private Double unitPrice;

  @Schema(description = "The amount of units for this charge item.", example = "34.4", minimum = "0", format = "float")
  private Double quantity;
}
