package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class Consignee {
  private UnspecifiedType partyName;

  private UnspecifiedType typeOfPerson;

  private PartyAddress address;

  private UnspecifiedType displayedAddress;

  private List<IdentifyingCode> identifyingCodes;

  private List<TaxLegalReference> taxLegalReferences;

  private List<PartyContactDetail> partyContactDetails;

  private UnspecifiedType reference;

  private UnspecifiedType purchaseOrderReferences;
}
