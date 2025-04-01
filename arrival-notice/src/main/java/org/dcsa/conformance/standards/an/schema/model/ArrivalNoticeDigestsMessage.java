package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "API message containing a list of Arrival Notice digests.")
public class ArrivalNoticeDigestsMessage {

  @Schema(description = "List of Arrival Notice digests.")
  private List<ArrivalNoticeDigest> arrivalNoticeDigests;
}
