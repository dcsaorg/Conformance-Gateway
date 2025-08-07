package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;
import org.dcsa.conformance.specifications.standards.an.v100.types.CurrencyCode;

@Schema(
    description =
        org.dcsa.conformance.specifications.standards.dt.v100.model.Charge.CLASS_SCHEMA_DESCRIPTION)
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearSchemaConstraints
public class Charge extends org.dcsa.conformance.specifications.standards.dt.v100.model.Charge {

  @Schema() protected CurrencyCode paymentCurrency;

  @Schema(
      maxLength = 50,
      example = "1.00 Euro = 1.17801 US Dollars",
      description =
"""
The quotation of the relative value of a currency unit against the unit of another currency
in the foreign exchange market applicable to this charge item.
The currency exchange specified here refers to the exchange rate between the payment currency
and the charge currency, in an unspecified order.

The Carrier Rate of Exchange applicability is defined based on the local jurisdiction.
If not provided, the customer should refer to the ROE available on the carrier website
or the ROE defined by the competent customs authority, as per local practice.

The publisher must clearly indicate in its API documentation whether the specified rate corresponds
to a vessel rate of exchange or a daily rate of exchange.
""")
  protected String carrierRateOfExchange;

  @Schema(description = PAYMENT_TERM_CODE_DESCRIPTION, example = "PRE")
  protected String paymentTermCode;

  @Schema(maxLength = 50, example = "FRT00", description = "Code associated with the charge")
  protected String chargeCode;

  @Schema(description = "Payment location")
  protected Location paymentLocation;

  @Schema(example = "000123456", description = "Code of the partner responsible for the payment")
  protected String chargePartnerCode;
}
