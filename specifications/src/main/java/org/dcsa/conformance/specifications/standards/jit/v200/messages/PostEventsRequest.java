package org.dcsa.conformance.specifications.standards.jit.v200.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.jit.v200.model.Event;

@Data
@Schema(description = "API message containing a list of events.")
public class PostEventsRequest {

  @Schema(description = "List of events.")
  private List<Event> events;
}
