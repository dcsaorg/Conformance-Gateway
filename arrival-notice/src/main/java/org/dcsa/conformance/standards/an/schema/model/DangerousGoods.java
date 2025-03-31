package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(
    description =
        """
Specification for Dangerous Goods.
 It is mandatory to provide one of UNNumber or NANumber.
 Dangerous Goods is based on IMDG Amendment Version 41-22.""")
public class DangerousGoods {

  @Schema(
      description =
          """
Four-character code supplied by Exis Technologies that assists to remove ambiguities when identifying a variant
 within a single UN number or NA number that may occur when two companies exchange DG information:
 * Character 1
   * Valid characters: 0, 1, 2, 3
   * Description: The packing group. Code 0 indicates there is no packing group.
 * Character 2
   * Valid characters: 0 to 9 and A to Z
   * Description: A sequence letter for the PSN, or 0 if there were no alternative PSNs.
 * Characters 3 and 4
   * Valid characters: 0 to 9 and A to Z
   * Description: Two sequence letters for other information, for the cases where the variant is required
    because of different in subrisks, packing instruction etc.""",
      example = "2200")
  private String codedVariantList;

  @Schema(
      description =
          """
The proper shipping name for goods under IMDG Code, or the product name for goods under IBC Code and IGC Code,
 or the bulk cargo shipping name for goods under IMSBC Code, or the name of oil for goods under Annex I to
 the MARPOL Convention.""",
      example = "Chromium Trioxide, anhydrous")
  private String properShippingName;

  @Schema(
      description =
          """
The recognized chemical or biological name or other name currently used for the referenced dangerous goods
 as described in chapter 3.1.2.8 of the IMDG Code.""",
      example = "xylene and benzene")
  private String technicalName;

  @Schema(
      description =
          """
The hazard class code of the referenced dangerous goods according to the specified regulation.
 Examples of possible values are:
 * 1.1A (Substances and articles which have a mass explosion hazard)
 * 1.6N (Extremely insensitive articles which do not have a mass explosion hazard)
 * 2.1 (Flammable gases)
 * 8 (Corrosive substances)
""",
      example = "1.4S")
  private String imoClass;

  @Schema(
      description =
          "Any risk in addition to the class of the referenced dangerous goods according to the IMO IMDG Code.",
      example = "1.2")
  private String subsidiaryRisk1;

  @Schema(
      description =
          "Any risk in addition to the class of the referenced dangerous goods according to the IMO IMDG Code.",
      example = "1.2")
  private String subsidiaryRisk2;

  @Schema(
      name = "isMarinePollutant",
      description = "Indicates if the goods belong to the classification of Marine Pollutant.",
      example = "true")
  private String marinePollutant;

  @Schema(
      description =
          "The packing group according to the UN Recommendations on the Transport of Dangerous Goods and IMO IMDG Code.",
      example = "3")
  private String packingGroup;

  @Schema(
      name = "isLimitedQuantity",
      description =
          """
Indicates if the dangerous goods can be transported as limited quantity
 in accordance with Chapter 3.4 of the IMO IMDG Code.""",
      example = "true")
  private String limitedQuantity;

  @Schema(
      name = "isExceptedQuantity",
      description =
          """
Indicates if the dangerous goods can be transported as excepted quantity
 in accordance with Chapter 3.5 of the IMO IMDG Code.""",
      example = "true")
  private String exceptedQuantity;

  @Schema(
      name = "isSalvagePackings",
      description =
          """
Indicates if the cargo has special packaging for the transport, recovery or disposal of damaged, defective,
 leaking or nonconforming hazardous materials packages, or hazardous materials that have spilled or leaked.""",
      example = "true")
  private String salvagePackings;

  @Schema(
      name = "isEmptyUncleanedResidue",
      description = "Indicates if the cargo is residue.",
      example = "true")
  private String emptyUncleanedResidue;

  @Schema(name = "isWaste", description = "Indicates if waste is being shipped.", example = "true")
  private String waste;

  @Schema(
      name = "isHot",
      description = "Indicates if high temperature cargo is being shipped.",
      example = "true")
  private String hot;

  @Schema(
      name = "isCompetentAuthorityApprovalRequired",
      description = "Indicates if the cargo require approval from authorities.",
      example = "true")
  private String competentAuthorityApprovalRequired;

  @Schema(
      description =
          "Name and reference number of the competent authority providing the approval.") // FIXME
  private String competentAuthorityApproval;

  @Schema(
      description =
          """
List of the segregation groups applicable to specific hazardous goods according to the IMO IMDG Code.
 (Only applicable to specific hazardous goods.)

Each element is a grouping of Dangerous Goods having certain similar chemical properties. Possible values are:
 * 1 (Acids)
 * 2 (Ammonium Compounds)
 * 3 (Bromates)
 * 4 (Chlorates)
 * 5 (Chlorites)
 * 6 (Cyanides)
 * 7 (Heavy metals and their salts)
 * 8 (Hypochlorites)
 * 9 (Lead and its compounds)
 * 10 (Liquid halogenated hydrocarbons)
 * 11 (Mercury and mercury compounds)
 * 12 (Nitrites and their mixtures)
 * 13 (Perchlorates)
 * 14 (Permanganates)
 * 15 (Powdered metals)
 * 16 (Peroxides),
 * 17 (Azides)
 * 18 (Alkalis)
""")
  private String segregationGroups;

  @Schema(
      description = "A list of Inner Packagings contained inside this outer packaging / overpack.")
  private List<InnerPackaging> innerPackagings;

  @Schema(description = "24-hour emergency contact details")
  private EmergencyContactDetails emergencyContactDetails;

  @Schema(
      name = "EMSNumber",
      description =
          """
The emergency schedule identified in the IMO EmS Guide – Emergency Response Procedures for Ships Carrying
 Dangerous Goods. Comprises 2 values; 1 for spillage and 1 for fire. Possible values spillage: S-A to S-Z.
 Possible values fire: F-A to F-Z.""",
      example = "F-A S-Q")
  private String emsNumber;

  @Schema(
      description = "Date by when the refrigerated liquid needs to be delivered.",
      example = "2025-01-23")
  private String endOfHoldingTime;

  @Schema(
      description = "Date & time when the container was fumigated.",
      example = "2025-01-23T12:34:56Z")
  private String fumigationDateTime;

  @Schema(
      name = "isReportableQuantity",
      description =
          """
Indicates if a container of hazardous material is at the reportable quantity level.
 If true, a report to the relevant authority must be made in case of spill.""",
      example = "true")
  private String reportableQuantity;

  @Schema(
      description =
          """
The zone classification of the toxicity of the inhalant. Possible values are:
 * A (Hazard Zone A) can be assigned to specific gases and liquids
 * B (Hazard Zone B) can be assigned to specific gases and liquids
 * C (Hazard Zone C) can only be assigned to specific gases
 * D (Hazard Zone D) can only be assigned to specific gases
""",
      example = "A")
  private String inhalationZone;

  @Schema(description = "Total weight of the goods carried, including packaging.")
  private Weight grossWeight;

  @Schema(description = "Total weight of the goods carried, excluding packaging.")
  private Weight netWeight;

  @Schema(
      description =
          "Total weight of the explosive substances, without the packagings, casings, etc.")
  private Weight netExplosiveContent;

  @Schema(
      description =
          "The volume of the referenced dangerous goods. (Only applicable to liquids and gas.)")
  private Volume netVolume;

  @Schema(description = "Temperature limits")
  private TemperatureLimits limits;

  @Schema(
      name = "UNNumber",
      description =
          """
United Nations Dangerous Goods Identifier (UNDG) assigned by the UN Sub-Committee of Experts on the
 Transport of Dangerous Goods and shown in the IMO IMDG.""",
      example = "1463")
  private String unNumber;

  @Schema(
    name = "NANumber",
    description =
      """
Four-digit number that is assigned to dangerous, hazardous, and harmful substances by the United States
 Department of Transportation.""",
    example = "9037")
  private String naNumber;
}
