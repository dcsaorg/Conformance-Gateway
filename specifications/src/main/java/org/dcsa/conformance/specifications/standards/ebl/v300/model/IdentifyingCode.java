package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Code-based identifier for a party. Includes the provider, code, and list name.")
@Data
public class IdentifyingCode {

  @Schema(description = "Code list provider. Examples:\n- `WAVE`\n- `CARX`\n- `ESSD`\n- `W3C`\n- `GLEIF`\n- `DNB`\n- `DCSA`\n- `ZZZ`", example = "W3C", maxLength = 100)
  private String codeListProvider;

  @Schema(description = "The actual party code as defined by the provider.", example = "MSK", maxLength = 150)
  private String partyCode;

  @Schema(description = "The name of the code list or authority for the `partyCode` (e.g. `DID`, `LEI`, `DUNS`).", example = "DID", maxLength = 100)
  private String codeListName;
}
