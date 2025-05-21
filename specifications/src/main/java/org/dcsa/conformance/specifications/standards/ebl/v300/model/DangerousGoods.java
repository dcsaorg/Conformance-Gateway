package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Specification for `Dangerous Goods`. It is mandatory to provide one of `UNNumber` or `NANumber`. Dangerous Goods is based on **IMDG Amendment Version 41-22**.")
@Data
public class DangerousGoods {

  @Schema(description = "Four-character code supplied by Exis Technologies that assists to remove ambiguities when identifying a variant within a single UN number or NA number that may occur when two companies exchange DG information.", example = "2200", minLength = 4, maxLength = 4, pattern = "^[0-3][0-9A-Z]{3}$")
  private String codedVariantList;

  @Schema(description = "The proper shipping name for goods under IMDG Code, or the product name for goods under IBC Code and IGC Code, or the bulk cargo shipping name for goods under IMSBC Code, or the name of oil for goods under Annex I to the MARPOL Convention.", example = "Chromium Trioxide, anhydrous", maxLength = 250)
  private String properShippingName;

  @Schema(description = "The recognized chemical or biological name or other name currently used for the referenced dangerous goods as described in chapter 3.1.2.8 of the IMDG Code.", example = "xylene and benzene", maxLength = 250)
  private String technicalName;

  @Schema(description = "The hazard class code of the referenced dangerous goods according to the specified regulation.", example = "1.4S", maxLength = 4)
  private String imoClass;

  @Schema(description = "Any risk in addition to the class of the referenced dangerous goods according to the IMO IMDG Code.", example = "1.2", minLength = 1, maxLength = 3, pattern = "^[0-9](\\.[0-9])?$")
  private String subsidiaryRisk1;

  @Schema(description = "Any risk in addition to the class of the referenced dangerous goods according to the IMO IMDG Code.", example = "1.2", minLength = 1, maxLength = 3, pattern = "^[0-9](\\.[0-9])?$")
  private String subsidiaryRisk2;

  @Schema(description = "Indicates if the goods belong to the classification of Marine Pollutant.", example = "false")
  private Boolean isMarinePollutant;

  @Schema(description = "The packing group according to the UN Recommendations on the Transport of Dangerous Goods and IMO IMDG Code.", example = "3", minimum = "1", maximum = "3", format = "int32")
  private Integer packingGroup;

  @Schema(description = "Indicates if the dangerous goods can be transported as limited quantity in accordance with Chapter 3.4 of the IMO IMDG Code.", example = "false")
  private Boolean isLimitedQuantity;

  @Schema(description = "Indicates if the dangerous goods can be transported as excepted quantity in accordance with Chapter 3.5 of the IMO IMDG Code.", example = "false")
  private Boolean isExceptedQuantity;

  @Schema(description = "Indicates if the cargo has special packaging for the transport, recovery or disposal of damaged, defective, leaking or nonconforming hazardous materials packages, or hazardous materials that have spilled or leaked.", example = "false")
  private Boolean isSalvagePackings;

  @Schema(description = "Indicates if the cargo is residue.", example = "false")
  private Boolean isEmptyUncleanedResidue;

  @Schema(description = "Indicates if waste is being shipped", example = "false")
  private Boolean isWaste;

  @Schema(description = "Indicates if high temperature cargo is shipped.", example = "false")
  private Boolean isHot;

  @Schema(description = "Indicates if the cargo require approval from authorities", example = "false")
  private Boolean isCompetentAuthorityApprovalRequired;

  @Schema(description = "Name and reference number of the competent authority providing the approval.", example = "{Name and reference...}", maxLength = 70, pattern = "^\\S(?:.*\\S)?$")
  private String competentAuthorityApproval;

  @ArraySchema(schema = @Schema(description = "List of the segregation groups applicable to specific hazardous goods according to the IMO IMDG Code."))
  private List<String> segregationGroups;

  @Schema(description = "A list of `Inner Packings` contained inside this `outer packaging/overpack`.")
  private List<InnerPackaging> innerPackagings;

  @Schema
  private EmergencyContactDetails emergencyContactDetails;

  @Schema(description = "The emergency schedule identified in the IMO EmS Guide.", example = "F-A S-Q", maxLength = 7)
  private String EMSNumber;

  @Schema(description = "Date by when the refrigerated liquid needs to be delivered.", example = "2021-09-03", format = "date")
  private String endOfHoldingTime;

  @Schema(description = "Date & time when the container was fumigated", example = "2024-09-04T09:41:00Z", format = "date-time")
  private String fumigationDateTime;

  @Schema(description = "Indicates if a container of hazardous material is at the reportable quantity level.", example = "false")
  private Boolean isReportableQuantity;

  @Schema(description = "The zone classification of the toxicity of the inhalant.", example = "A", minLength = 1, maxLength = 1)
  private String inhalationZone;

  @Schema
  private GrossWeight grossWeight;

  @Schema
  private NetWeight netWeight;

  @Schema
  private NetExplosiveContent netExplosiveContent;

  @Schema
  private NetVolume netVolume;

  @Schema
  private Limits limits;
}
