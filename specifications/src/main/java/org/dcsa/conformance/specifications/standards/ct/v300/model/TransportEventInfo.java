package org.dcsa.conformance.specifications.standards.ct.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ct.v300.types.TransportEventTypeCode;

@Data
@Schema(description = "Transport event information")
public class TransportEventInfo {

  @Schema() private TransportEventTypeCode transportEventTypeCode;
}
