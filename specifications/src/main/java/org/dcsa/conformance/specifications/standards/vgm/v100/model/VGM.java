package org.dcsa.conformance.specifications.standards.vgm.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;

@Data
@Schema(description = "Verified Gross Mass submission for one piece of equipment")
public class VGM {

  @Schema(name = "VGMRouting")
  private VGMRouting vgmRouting;

  @Schema(
      name = "VGMID",
      maxLength = 500,
      example = "VGM-HHL71800000-APZU4812090-2025-01-23T01:23:45Z",
      description =
"""
ID of the VGM, unique among all the VGMs published by a VGM Producer.

A VGM overrides any other VGM that has the same `vgmID` and an earlier `updatedDateTime`.

Each VGM is uniquely identified within each VGM 1.x standard ecosystem of connected implementers
by a composite key including:
- `VGMRouting.originatingParty.partyCode`
- `VGMRouting.originatingParty.codeListProvider`
- `VGMRouting.originatingParty.codeListName`
- `VGMID`
""")
  private String vgmID;

  @Schema(
      description =
"""
Flag indicating that the VGM is retracted.

The data in this and all previously transmitted VGMs with the same `VGMID` must be discarded or ignored.

If this flag is set, any VGM data other than the `VGMID` is irrelevant (if present).
""")
  private Boolean isRetracted;

  @Schema(description = "The date and time when the VGM was last updated.")
  private FormattedDateTime updatedDateTime;
}
