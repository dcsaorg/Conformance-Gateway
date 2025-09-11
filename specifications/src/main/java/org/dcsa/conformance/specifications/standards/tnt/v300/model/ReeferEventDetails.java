package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.ReeferEventTypeCode;

@Schema(description = "Reefer-related information")
@Data
public class ReeferEventDetails {

  @Schema() private ReeferEventTypeCode reeferEventTypeCode;
}
