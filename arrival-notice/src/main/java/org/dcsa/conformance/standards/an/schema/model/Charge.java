package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Contact details")
public class Charge {

  @Schema(
      description = "Free text field describing the charge",
      example = "Documentation fee - Destination")
  private String chargeName;

  @Schema(description = "The ISO 4217 3-character code of the charge currency.", example = "EUR")
  private String currencyCode;

  @Schema(
      description =
          """
An indicator of whether a charge is prepaid (PRE) or collect (COL). When prepaid, the charge is the responsibility
 of the shipper or the Invoice payer on behalf of the shipper (if provided). When collect, the charge is
 the responsibility of the consignee or the Invoice payer on behalf of the consignee (if provided).
 * PRE (Prepaid)
 * COL (Collect)
""",
      example = "PRE")
  private String paymentTermCode;

  @Schema(
      description =
          """
The monetary value of all freight and other service charges for a transport document,
 with a maximum of 2-digit decimals.""",
      example = "123.45")
  private String currencyAmount;

  @Schema(
      description =
          """
The quotation of the relative value of a currency unit against the unit of another currency
 in the foreign exchange market applicable to this charge item. The Carrier rate of exchange applicability
 is defined based on the local jurisdiction. If not provided, the customer should refer to the ROE available
 on the carrier website or the ROE defined by the competent customs authority, as per local practice.""",
      example = "1.00 EUR = 1.08149 USD")
  private String carrierRateOfExchange;

  @Schema(
      description =
          """
The code specifying the measure unit used for the corresponding unit price for this cost, such as per day, per ton,
 per square metre.""",
      example = "Per day")
  private String calculationBasis;

  @Schema(
      description = "The unit price of this charge item in the currency of the charge.",
      example = "1234.56")
  private String unitPrice;

  @Schema(description = "The amount of unit for this charge item.", example = "12.3")
  private String quantity;

  @Schema(
      description = "Identifier of the invoice towards the consignee in relation to the shipment.",
      example = "INV51XX80YY")
  private String invoiceReference;
}
