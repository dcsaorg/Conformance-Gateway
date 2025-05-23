package org.dcsa.conformance.specifications.standards.dt.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The national commodity classification code linked to a country with a value. Examples: NCM (Brazil), HTS (US), TARIC (EU).")
@Data
public class NationalCommodityCode {

  @Schema(
      description =
"""
The national commodity classification code, which can be one of the following values defined by DCSA:
- `NCM` (Nomenclatura Comum do Mercosul)
- `HTS` (Harmonized Tariff Schedule)
- `SCHEDULE_B` ( Schedule B)
- `TARIC` (Integrated Tariff of the European Communities)
- `CN` (Combined Nomenclature)
- `CUS` (Customs Union and Statistics)
""",
      example = "NCM",
      maxLength = 10,
      pattern = "^\\S(?:.*\\S)?$")
  private String type;

  @Schema(
      description =
"""
The 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
""",
      example = "BR",
      minLength = 2,
      maxLength = 2,
      pattern = "^[A-Z]{2}$")
  private String countryCode;

  @Schema(description = "A list of `national commodity codes` values.")
  @ArraySchema(schema = @Schema(description = "A value of the National Commodity Code.", example = "1515", maxLength = 10, pattern = "^\\S(?:.*\\S)?$"), minItems = 1)
  private List<String> values;
}
