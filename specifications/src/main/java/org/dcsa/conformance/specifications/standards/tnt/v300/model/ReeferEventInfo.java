package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ReeferEventTypeCode;

@Data
@Schema(description = "Reefer event information")
public class ReeferEventInfo {

  @Schema() private ReeferEventTypeCode reeferEventTypeCode;
}
