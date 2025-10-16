package org.dcsa.conformance.specifications.standards.vgm.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.Party;

@Schema(
    description =
"""
Routing of an event from the originating party, via forwarding parties, to the destination party.
""")
@Data
public class VGMRouting {

  @Schema(
      description =
"""
The initial party with which the event originated.
""")
  private Party originatingParty;

  @Schema(
      description =
"""
Ordered list of parties forwarding the event between the originating and destination party.
""")
  private List<Party> forwardingParties;

  @Schema(
      description =
"""
The final party intended as destination of the event.
""")
  private Party destinationParty;
}
