package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

@Data
@Schema()
public class CarriersAgentAtDestination {
  private UnspecifiedType partyName;

  private PartyAddress address;

  private List<PartyContactDetail> partyContactDetails;
}
