package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class Party {
  private UnspecifiedType partyName;

  private PartyAddress address;

  private List<IdentifyingCode> identifyingCodes;

  private List<TaxLegalReference> taxLegalReferences;

  private List<PartyContactDetail> partyContactDetails;

  private UnspecifiedType reference;
}
