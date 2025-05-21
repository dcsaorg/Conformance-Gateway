package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "References for a consignment item, used by shippers or freight forwarders and shared in events or printed on documents.")
@Data
public class ReferenceConsignmentItem {

  @Schema(description = "Reference type code defined by DCSA.\n- `CR` (Customer’s Reference)\n- `AKG` (Vehicle Identification Number)\n- `SPO` (Shipper's Purchase Order)\n- `CPO` (Consignee's Purchase Order)", example = "CR", maxLength = 3)
  private String type;

  @ArraySchema(schema = @Schema(description = "A value for the reference.", example = "HHL00103004", maxLength = 35, pattern = "^\\S(?:.*\\S)?$"), minItems = 1)
  private List<String> values;
}
