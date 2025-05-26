package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "References for a consignment item, used by shippers or freight forwarders and shared in events or printed on documents.")
@Data
public class ReferenceConsignmentItem {

  @Schema(
      description =
"""
The reference type codes defined by DCSA. Possible values are:
- `CR` (Customer’s Reference)
- `AKG` (Vehicle Identification Number)
- `SPO` (Shipper's Purchase Order)
- `CPO` (Consignee's Purchase Order)
""",
      example = "CR",
      maxLength = 3)
  private String type;

  @Schema(description = "List of `referenceValues` for a given `referenceType`.")
  @ArraySchema(schema = @Schema(description = "A value for the reference.", example = "HHL00103004", maxLength = 35, pattern = "^\\S(?:.*\\S)?$"), minItems = 1)
  private List<String> values;
}
