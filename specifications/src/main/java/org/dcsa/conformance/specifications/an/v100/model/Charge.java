package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.CurrencyAmount;
import org.dcsa.conformance.specifications.an.v100.types.CurrencyCode;
import org.dcsa.conformance.specifications.an.v100.types.FreightPaymentStatus;
import org.dcsa.conformance.specifications.an.v100.types.PaymentTermCode;

@Data
@Schema(description = "Charge details")
public class Charge {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50,
      description = "Description of the charge",
      example = "Documentation fee - Destination")
  private String chargeDescription;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private PaymentTermCode paymentTermCode;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "The monetary value of this charge, expressed with a maximum of 2 decimal digits")
  private CurrencyAmount chargeAmount;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private CurrencyCode chargeCurrency;

  @Schema(
      maxLength = 50,
      example = "1.00 EUR = 1.08149 USD",
      description =
"""
The quotation of the relative value of a currency unit against the unit of another currency
in the foreign exchange market applicable to this charge item. The Carrier rate of exchange applicability
is defined based on the local jurisdiction. If not provided, the customer should refer to the ROE available
on the carrier website or the ROE defined by the competent customs authority, as per local practice.
""")
  private String carrierRateOfExchange;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50,
      description =
"""
The code specifying the measure unit used for the corresponding unit price for this cost, such as per day, per ton,
per square metre.
""",
      example = "Per day")
  private String calculationBasis;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
The unit price of this charge item in the `chargeCurrency`, expressed with a maximum of 2 decimal digits
""")
  private CurrencyAmount unitPrice;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      pattern = "^\\d+(\\.\\d+)?$",
      example = "123.456",
      description = "The quantity of this charge item in units at `unitPrice`")
  private String quantity;

  @Schema(
      type = "string",
      maxLength = 100,
      example = "INV51XX80YY",
      description = "Reference of the invoice towards the consignee for this shipment charge")
  private String invoiceReference;

  @Schema()
  private FreightPaymentStatus freightPaymentStatus;
}
