package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(
    description = "The national commodity classification code linked to a country with a value.")
public class NationalCommodityCode {

  @Schema(
      description =
"""
The national commodity classification code, which can be one of the following values defined by DCSA:
 * NCM (Nomenclatura Comum do Mercosul)
 * HTS (Harmonized Tariff Schedule)
 * SCHEDULE_B ( Schedule B)
 * TARIC (Integrated Tariff of the European Communities)
 * CN (Combined Nomenclature)
 * CUS (Customs Union and Statistics)
""",
      example = "NCM")
  private String nccType;

  @Schema(
      description =
"""
The 2 characters for the country code using ISO 3166-1 alpha-2
""",
      example = "BR")
  private String countryCode;

  @Schema(
      description =
"""
A list of national commodity codes values
""",
      example = "[1234, 5678]")
  private List<String> values;
}
