package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.EventTypeCode;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.TransportEventTypeCode;

@Schema(description = "Transport event")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
public class TransportEvent extends Event {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = "TRANSPORT")
  private EventTypeCode eventTypeCode;

  @Schema() private TransportEventTypeCode transportEventTypeCode;

  @Schema(
      example = "Bad weather",
      description =
"""
Free text information provided by the vessel operator regarding the reasons for the change in schedule
and/or plans to mitigate schedule slippage.
""",
      maxLength = 250)
  private String changeRemark;

  @Schema(
      description =
          """
      Code identifying the reason for the delay, as defined by SMDG here:
      https://smdg.org/documents/smdg-code-lists/delay-reason-and-port-call-activity/
      """)
  private String delayReasonCode;
}
