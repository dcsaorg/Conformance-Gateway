package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.IotEventTypeCode;

@Schema(description = "IoT-related information")
@Data
public class IotEventDetails {

  @Schema() private IotEventTypeCode iotEventTypeCode;
}
