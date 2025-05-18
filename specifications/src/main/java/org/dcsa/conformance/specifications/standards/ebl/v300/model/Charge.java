package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class Charge {
  private UnspecifiedType chargeName;

  private UnspecifiedType currencyAmount;

  private UnspecifiedType currencyCode;

  private UnspecifiedType paymentTermCode;

  private UnspecifiedType calculationBasis;

  private UnspecifiedType unitPrice;

  private UnspecifiedType quantity;
}
