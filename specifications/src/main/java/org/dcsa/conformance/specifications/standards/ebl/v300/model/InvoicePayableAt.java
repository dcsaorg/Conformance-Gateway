package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Location where payment of ocean freight and charges for the main transport will take place by the customer. Can be a UN Location Code or free-text name.")
@Data
public class InvoicePayableAt {

  @Schema(description = "The UN Location code specifying where the place is located. Must follow UN/LOCODE standard.", example = "NLAMS", minLength = 5, maxLength = 5, pattern = "^[A-Z]{2}[A-Z2-9]{3}$")
  private String UNLocationCode;

  @Schema(description = "The name of the location where payment will be rendered by the customer.", example = "DCSA Headquarters", maxLength = 35)
  private String freeText;
}
