package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.TransportEventTypeCode;

@Schema(description = "Transport-specific event details")
@Data
public class TransportEventDetails {

  @Schema() private TransportEventTypeCode transportEventTypeCode;

  @Schema(
      description =
"""
Code identifying the reason for the delay, as defined by SMDG here:
https://smdg.org/documents/smdg-code-lists/delay-reason-and-port-call-activity/
""")
  private String delayReasonCode;

  @Schema(
      example = "Bad weather",
      description =
"""
Free text information provided by the vessel operator regarding the reasons for the change in schedule
and/or plans to mitigate schedule slippage.
""",
      maxLength = 250)
  private String changeRemark;

  @Schema() private TransportCall transportCall;
}
