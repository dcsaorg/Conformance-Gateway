package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "API message containing a list of Arrival Notice lightweight notifications.")
public class ArrivalNoticeNotificationsMessage {

  @Schema(description = "List of Arrival Notice lightweight notifications.")
  private List<ArrivalNoticeNotification> arrivalNoticeNotifications;
}
