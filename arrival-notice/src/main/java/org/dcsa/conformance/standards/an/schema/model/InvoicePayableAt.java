package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description =
        "Location where payment of ocean freight and charges for the main transport will take place by the customer.")
public class InvoicePayableAt {

  @Schema(
      name = "UNLocationCode",
      description =
          """
The UN Location code specifying where the place is located. The pattern used must be:
 * 2 characters for the country code using ISO 3166-1 alpha-2
 * 3 characters to code a location within that country. Letters A-Z and numbers from 2-9 can be used
More info can be found at: https://unece.org/trade/cefact/UNLOCODE-Download""",
      example = "NLAMS")
  private String unLocationCode;

  @Schema(
      description = "The name of the location where payment will be rendered by the customer.",
      example = "DCSA HQ")
  private String freeText;
}
