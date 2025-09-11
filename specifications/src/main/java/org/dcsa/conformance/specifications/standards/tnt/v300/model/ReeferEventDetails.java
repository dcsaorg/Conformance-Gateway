package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.ActiveReeferParameters;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ReeferEventTypeCode;

@Schema(description = "Reefer-related information")
@Data
public class ReeferEventDetails {

  @Schema() private ReeferEventTypeCode reeferEventTypeCode;

  @Schema(description = "Setpoint active reefer parameters")
  private ActiveReeferParameters setpointParameters;

  @Schema(description = "Measured active reefer parameters")
  private ActiveReeferParameters measuredParameters;
}
