package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.constraints.AtLeastOneAttributeIsRequired;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.standards.an.v100.types.DocumentPartyTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.PersonTypeCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Address;
import org.dcsa.conformance.specifications.standards.dt.v100.model.PartyContactDetail;
import org.dcsa.conformance.specifications.standards.dt.v100.model.TaxLegalReference;

@Data
@Schema(description = "Document party")
public class DocumentParty {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private DocumentPartyTypeCode partyType;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 70,
      description = "Party name",
      example = "Acme Inc.")
  private String name;

  @Schema() private PersonTypeCode personType;

  @Schema() private Address address;

  @Schema(description = "List of codes identifying the party")
  @ArraySchema(minItems = 1)
  private List<IdentifyingPartyCode> identifyingCodes;

  @Schema(description = "List of tax or legal references relevant to the party")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "Party contact details")
  private List<PartyContactDetail> contactDetails;

  public static List<SchemaConstraint> getConstraints() {
    return List.of(
        new AtLeastOneAttributeIsRequired(
            List.of(
                SpecificationToolkit.getClassField(DocumentParty.class, "address"),
                SpecificationToolkit.getClassField(DocumentParty.class, "identifyingCodes"))));
  }
}
