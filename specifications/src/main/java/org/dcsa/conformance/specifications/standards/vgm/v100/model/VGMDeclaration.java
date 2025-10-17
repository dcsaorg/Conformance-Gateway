package org.dcsa.conformance.specifications.standards.vgm.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.core.v100.model.PartyDetails;
import org.dcsa.conformance.specifications.standards.core.v100.types.FormattedDateTime;

@Data
@Schema(
    description =
"""
VGM declaration for one piece of equipment, including the actual VGM and the relevant associated details
""")
public class VGMDeclaration {

  @Schema(name = "VGMRouting")
  private RoutingDetails routingDetails;

  @Schema(
      maxLength = 500,
      example = "VGM-HHL71800000-APZU4812090-2025-01-23T01:23:45Z",
      description = "Reference of the VGM declaration")
  private String declarationReference;

  @Schema(
      description =
"""
Flag indicating that the VGM is retracted.

The data in this and all previously transmitted VGMs with the same `VGMID` must be discarded or ignored.

If this flag is set, any VGM data other than the `VGMID` is irrelevant (if present).
""")
  private Boolean isRetracted;

  @Schema(description = "The date and time when the VGM declaration was last updated")
  private FormattedDateTime declarationDateTime;

  @Schema(name = "VGM")
  private VGM vgm;

  @Schema() private EquipmentDetails equipmentDetails;

  @Schema() private ShipmentDetails shipmentDetails;

  @Schema() private VesselVoyageDetails vesselVoyageDetails;

  @Schema(description = "Party responsible for the VGM declaration.")
  private PartyDetails responsibleParty;

  @Schema(description = "Party authorized to act on behalf of the `responsibleParty`.")
  private PartyDetails authorizedParty;

  @Schema(maxLength = 255, example = "Jane Doe", description = "Authorized person signatory")
  private String authorizedPersonSignatory;

  @Schema(description = "Legally accepted party designated to perform the weighing.")
  private PartyDetails weighingParty;

  @Schema(description = "Party that submits the VGM declaration.")
  private PartyDetails submittingParty;

  @Schema(description = "Party that orders the weighing at the terminal or weighing station.")
  private PartyDetails orderedByParty;

  @Schema(description = "Information reference agency holding the VGM declaration.")
  private PartyDetails holdingParty;
}
