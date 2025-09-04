package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ReeferEventTypeCode;

@Schema(description = "Reefer event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class ReeferEvent extends Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "REEFER")
  private EventTypeCode eventTypeCode;

  @Schema() private ReeferEventTypeCode reeferEventTypeCode;
}
