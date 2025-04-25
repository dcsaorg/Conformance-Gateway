package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(
    description = "The national commodity classification code linked to a country with a value.")
public class NationalCommodityCode {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
      maxLength = 10,
      example = "NCM",
      description =
"""
The national commodity classification code, which can be one of the following values defined by DCSA:
 * NCM (Nomenclatura Comum do Mercosul)
 * HTS (Harmonized Tariff Schedule)
 * SCHEDULE_B ( Schedule B)
 * TARIC (Integrated Tariff of the European Communities)
 * CN (Combined Nomenclature)
 * CUS (Customs Union and Statistics)
""")
  private String nccType;

  @Schema() private CountryCode countryCode;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "List of national commodity code values")
  @ArraySchema(minItems = 1)
  private List<NationalCommodityCodeValue> nccValues;
}
