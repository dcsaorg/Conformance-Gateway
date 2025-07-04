package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

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

  @Schema(
      maxLength = 50,
      example = "1.00 Euro = 1.17801 US Dollars",
      description =
"""
The quotation of the relative value of a currency unit against the unit of another currency
in the foreign exchange market applicable to this charge item.
The Carrier rate of exchange applicability is defined based on the local jurisdiction.
If not provided, the customer should refer to the ROE available on the carrier website
or the ROE defined by the competent customs authority, as per local practice.
""")
  protected String carrierRateOfExchange;

  @Schema(description = PAYMENT_TERM_CODE_DESCRIPTION, example = "PRE")
  protected String paymentTermCode;
}
