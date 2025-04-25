package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.an.v100.types.SegregationGroupCode;
import org.dcsa.conformance.specifications.an.v100.types.SubsidiaryRisk;

import java.util.List;

@Data
@Schema(
    description =
"""
Specification for dangerous goods based on the IMDG Amendment Version 41-22
""")
public class DangerousGoods {

  @Schema(
      type = "string",
      pattern = "^\\d{4}$",
      minLength = 4,
      maxLength = 4,
      example = "1463",
      description =
"""
United Nations Dangerous Goods (UNDG) Identifier assigned by the UN Sub-Committee of Experts on the
Transport of Dangerous Goods and shown in the IMO IMDG.
""")
  private String unNumber;

  @Schema(
      type = "string",
      pattern = "^\\d{4}$",
      minLength = 4,
      maxLength = 4,
      example = "9037",
      description =
"""
Four-digit number that is assigned to dangerous, hazardous, and harmful substances by the United States
Department of Transportation.
""")
  private String naNumber;

  @Schema(
      type = "string",
      pattern = "^[0-3][0-9A-Z]{3}$",
      minLength = 4,
      maxLength = 4,
      example = "2200",
      description =
"""
Four-character code supplied by Exis Technologies that assists to remove ambiguities when identifying a variant
within a single UN number or NA number that may occur when two companies exchange DG information:

Character | Valid Characters | Description
:--------:|------------------|------------
1| 0, 1, 2, 3|The packing group. Code 0 indicates there is no packing group
2|0 to 9 and A to Z|A sequence letter for the PSN, or 0 if there were no alternative PSNs
3 and 4|0 to 9 and A to Z|Two sequence letters for other information, for the cases where the variant is required because of different in subrisks, packing instruction etc.
""")
  private String codedVariantList;

  @Schema(
      type = "string",
      maxLength = 250,
      example = "Chromium Trioxide, anhydrous",
      description =
"""
The proper shipping name for goods under IMDG Code, or the product name for goods under IBC Code and IGC Code,
or the bulk cargo shipping name for goods under IMSBC Code, or the name of oil for goods under Annex I to
the MARPOL Convention.
""")
  private String properShippingName;

  @Schema(
      type = "string",
      maxLength = 250,
      example = "xylene and benzene",
      description =
"""
The recognized chemical or biological name or other name currently used for the referenced dangerous goods
as described in chapter 3.1.2.8 of the IMDG Code.
""")
  private String technicalName;

  @Schema(
      example = "1.4S",
      description =
"""
The hazard class code of the referenced dangerous goods according to the specified regulation.

Examples of possible values are:
 * 1.1A (Substances and articles which have a mass explosion hazard)
 * 1.6N (Extremely insensitive articles which do not have a mass explosion hazard)
 * 2.1 (Flammable gases)
 * 8 (Corrosive substances)
""")
  private String imoClass;

  private SubsidiaryRisk subsidiaryRisk1;

  private SubsidiaryRisk subsidiaryRisk2;

  @Schema(
      name = "isMarinePollutant",
      type = "boolean",
      example = "false",
      description = "Indicates whether the goods belong to the classification of Marine Pollutant.")
  private String marinePollutant;

  @Schema(
      type = "integer",
      format = "int32",
      minimum = "1",
      maximum = "3",
      example = "3",
      description =
"""
The packing group according to the UN Recommendations on the Transport of Dangerous Goods and IMO IMDG Code.
""")
  private String packingGroup;

  @Schema(
      name = "isLimitedQuantity",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if the dangerous goods can be transported as limited quantity
in accordance with Chapter 3.4 of the IMO IMDG Code.
""")
  private String limitedQuantity;

  @Schema(
      name = "isExceptedQuantity",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if the dangerous goods can be transported as excepted quantity
in accordance with Chapter 3.5 of the IMO IMDG Code.
""")
  private String exceptedQuantity;

  @Schema(
      name = "isSalvagePackings",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if the cargo has special packaging for the transport, recovery or disposal of damaged, defective,
leaking or nonconforming hazardous materials packages, or hazardous materials that have spilled or leaked.
""")
  private String salvagePackings;

  @Schema(
      name = "isEmptyUncleanedResidue",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if the cargo is residue.
""")
  private String emptyUncleanedResidue;

  @Schema(
      name = "isWaste",
      type = "boolean",
      example = "true",
      description =
"""
Indicates if waste is being shipped.
""")
  private String waste;

  @Schema(
      name = "isHot",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if high temperature cargo is being shipped.
""")
  private String hot;

  @Schema(
      name = "isCompetentAuthorityApprovalRequired",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if the cargo require approval from authorities.
""")
  private String competentAuthorityApprovalRequired;

  @Schema(
      type = "string",
      maxLength = 70,
      description =
"""
Name and reference number of the competent authority providing the approval
""")
  private String competentAuthorityApproval;

  @Schema(
      description =
"""
List of the segregation groups applicable to specific hazardous goods according to the IMO IMDG Code.
(Only applicable to specific hazardous goods.)
""")
  private List<SegregationGroupCode> segregationGroups;

  @Schema(
      description =
"""
List of inner packagings contained inside this outer packaging / overpack
""")
  private List<InnerPackaging> innerPackagings;

  @Schema() private EmergencyContactDetails emergencyContactDetails;

  @Schema(
      type = "string",
      maxLength = 7,
      example = "F-A S-Q",
      description =
"""
The emergency schedule identified in the IMO EmS Guide – Emergency Response Procedures
for Ships Carrying Dangerous Goods.

Comprises 2 values: 1 for spillage and 1 for fire.

Possible values spillage: S-A to S-Z.

Possible values fire: F-A to F-Z.
""")
  private String emsNumber;

  @SchemaOverride(description = "Date by when the refrigerated liquid needs to be delivered")
  private FormattedDate endOfHoldingTime;

  @SchemaOverride(description = "Date & time when the container was fumigated.")
  private FormattedDateTime fumigationDateTime;

  @Schema(
      name = "isReportableQuantity",
      type = "boolean",
      example = "false",
      description =
"""
Indicates if a container of hazardous material is at the reportable quantity level.
If true, a report to the relevant authority must be made in case of spill.
""")
  private String reportableQuantity;

  @Schema()
  private String inhalationZone;

  @SchemaOverride(description = "Total weight of the goods carried, including packaging.")
  private Weight grossWeight;

  @Schema(description = "Total weight of the goods carried, excluding packaging.")
  private Weight netWeight;

  @Schema(
      description =
          "Total weight of the explosive substances, without the packagings, casings, etc.")
  private Weight netExplosiveContent;

  @Schema(
      description =
          "Volume of the referenced dangerous goods (only applicable to liquids and gas)")
  private Volume netVolume;

  @Schema()
  private TemperatureLimits limits;
}
