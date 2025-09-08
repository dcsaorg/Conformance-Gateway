package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.IotEventTypeCode;

@Data
@Schema(description = "IoT event information")
public class IotEventInfo {

  @Schema() private IotEventTypeCode iotEventTypeCode;
}
