package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.IotEventTypeCode;

@Schema(description = "IoT event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class IotEvent extends Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "IOT")
  private EventTypeCode eventTypeCode;

  @Schema() private IotEventTypeCode iotEventTypeCode;
}
