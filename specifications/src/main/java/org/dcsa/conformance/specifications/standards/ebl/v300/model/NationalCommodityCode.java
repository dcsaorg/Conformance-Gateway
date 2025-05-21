package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The national commodity classification code linked to a country with a value. Examples: NCM (Brazil), HTS (US), TARIC (EU).")
@Data
public class NationalCommodityCode {

  @Schema(description = "The type of national commodity classification code.\n- `NCM`\n- `HTS`\n- `SCHEDULE_B`\n- `TARIC`\n- `CN`\n- `CUS`", example = "NCM", maxLength = 10, pattern = "^\\S(?:.*\\S)?$")
  private String type;

  @Schema(description = "Country code where the classification applies (ISO 3166-1 alpha-2).", example = "BR", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;

  @ArraySchema(schema = @Schema(description = "A value of the National Commodity Code.", example = "1515", maxLength = 10, pattern = "^\\S(?:.*\\S)?$"), minItems = 1)
  private List<String> values;
}
