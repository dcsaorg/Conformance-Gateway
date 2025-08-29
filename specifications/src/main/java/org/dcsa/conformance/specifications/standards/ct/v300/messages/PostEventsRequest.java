package org.dcsa.conformance.specifications.standards.ct.v300.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ct.v300.model.ContainerTrackingEvent;

@Data
@Schema(description = "API message containing a list of container tracking events.")
public class PostEventsRequest {

  @Schema(description = "List of container tracking events.")
  private List<ContainerTrackingEvent> events;
}
