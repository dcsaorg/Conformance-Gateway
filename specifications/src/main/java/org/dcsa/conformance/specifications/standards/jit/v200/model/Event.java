package org.dcsa.conformance.specifications.standards.jit.v200.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.UniversallyUniqueID;

@Data
@Schema(description = "Just in Time Port Call event")
public class Event {
  @Schema(description = "Universally unique ID of the port call.")
  private UniversallyUniqueID portCallID;
}
