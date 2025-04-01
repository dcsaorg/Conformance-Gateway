package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "API message containing a list of Arrival Notices.")
public class ArrivalNoticesMessage {

  @Schema(description = "List of arrival notices.")
  private List<ArrivalNotice> arrivalNotices;
}
