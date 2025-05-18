package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema()
public class DocumentParties {
  private Shipper shipper;

  private Consignee consignee;

  private Endorsee endorsee;

  private IssuingParty issuingParty;

  private CarriersAgentAtDestination carriersAgentAtDestination;

  private List<NotifyParty> notifyParties;

  private List<OtherDocumentParty> other;
}
