package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.ClearSchemaConstraints;

@Schema(
    description =
"""
Reference used by the customer or carrier to identify or track the shipment.
""")
@ClearSchemaConstraints
public class Reference
    extends org.dcsa.conformance.specifications.standards.dt.v100.model.Reference {

  @Schema(
      description =
"""
The reference type codes defined by DCSA. Possible values are:
- `CA` (Carrier’s Reference)
- `CR` (Customer’s Reference)
- `AKG` (Vehicle Identification Number)
- `CPR` (Canadian Pacific Railway System)
- `CNR` (Canadian National Railway Unit)
""",
      example = "CR",
      maxLength = 3)
  protected String type;
}
