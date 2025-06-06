package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.dt.v100.model.IdentifyingCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.PartyContactDetail;
import org.dcsa.conformance.specifications.standards.dt.v100.model.TaxLegalReference;
import org.dcsa.conformance.specifications.standards.dt.v100.types.DisplayedAddressLine;
import org.dcsa.conformance.specifications.standards.dt.v100.types.PurchaseOrderReference;

@Data
@Schema(description = "Document party")
public class DocumentParty {

  @Schema(
      description =
"""
Specifies the role of the party in a given context. Possible values are:

- `SHP` (Shipper)
- `CSG` (Consignee)
- `CSG` (Endorsee)
- `ISS` (Issuing Party)
- `CAG` (Carrier's Agent at Destination)
- `NP1` (First Notify Party)
- `NP2` (Second Notify Party)
- `NPX` (Other Notify Party)
- `SCO` (Service Contract Owner)
- `DDR` (Consignor's freight forwarder)
- `DDS` (Consignee's freight forwarder)
- `COW` (Invoice payer on behalf of the consignor (shipper))
- `COX` (Invoice payer on behalf of the consignee)
- `CS` (Consolidator)
- `MF` (Manufacturer)
- `WH` (Warehouse Keeper)
""",
      example = "DDS",
      maxLength = 3)
  private String partyFunction;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 70,
      description = "Party name",
      example = "Acme Inc.")
  private String partyName;

  @Schema(
      description =
          """
      Can be one of the following values as per the Union Customs Code art. 5(4):
      - `NATURAL_PERSON` (A person that is an individual living human being)
      - `LEGAL_PERSON` (person (including a human being and public or private organizations) that can perform legal actions, such as own a property, sue and be sued)
      - `ASSOCIATION_OF_PERSONS` (Not a legal person, but recognised under Union or National law as having the capacity to perform legal acts)
      """,
      example = "NATURAL_PERSON",
      maxLength = 50,
      pattern = "^\\S(?:.*\\S)?$")
  private String typeOfPerson;

  @Schema(description = "Party location")
  private Location location;

  @Schema(
      description =
          """
      The address of the party to be displayed on the `Transport Document`. The displayed address may be used to match the address provided in the `Letter of Credit`.

      **Conditions:** If provided:
        - the displayed address must be included in the `Transport Document`.
        - for physical BL (`isElectronic=false`), it is only allowed to provide max 2 lines of 35 characters
        - for electronic BL (`isElectronic=true`), the limit is 6 lines of 35 characters
        - the order of the items in this array **MUST** be preserved as by the provider of the API.
      """)
  @ArraySchema(maxItems = 6)
  private List<DisplayedAddressLine> displayedAddress;

  @Schema(description = "List of codes identifying the party")
  @ArraySchema(minItems = 1)
  private List<IdentifyingCode> identifyingCodes;

  @Schema(description = "List of tax or legal references relevant to the party")
  private List<TaxLegalReference> taxLegalReferences;

  @Schema(description = "Party contact details")
  private List<PartyContactDetail> partyContactDetails;

  @Schema(
      description = "A reference linked to the `Consignee`.",
      example = "HHL007",
      maxLength = 35,
      pattern = "^\\S(?:.*\\S)?$")
  private String reference;

  @Schema(description = "A list of `Purchase Order Reference`s")
  private List<PurchaseOrderReference> purchaseOrderReferences;
}
