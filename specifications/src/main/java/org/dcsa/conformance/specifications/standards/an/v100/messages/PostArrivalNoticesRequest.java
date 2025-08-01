package org.dcsa.conformance.specifications.standards.an.v100.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNotice;

@Data
@Schema(description = "API message containing a list of Arrival Notices.")
public class PostArrivalNoticesRequest {

  @Schema(description = "List of arrival notices.")
  private List<ArrivalNotice> arrivalNotices;
}
