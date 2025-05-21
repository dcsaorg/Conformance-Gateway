package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Outer packaging or overpack specification, such as palletized or crated groupings of packages.")
@Data
public class OuterPackaging {

  @Schema(description = "Code identifying the outer packaging (per UN Recommendation 21).", example = "5H", minLength = 2, maxLength = 2, pattern = "^[A-Z0-9]{2}$")
  private String packageCode;

  @Schema(description = "IMO packaging code as per the IMDG Code.", example = "1A2", minLength = 1, maxLength = 5, pattern = "^[A-Z0-9]{1,5}$")
  private String imoPackagingCode;

  @Schema(description = "Number of outer packagings or overpacks.", example = "18", minimum = "1", maximum = "99999999", format = "int32")
  private Integer numberOfPackages;

  @Schema(description = "Textual description of the packaging.", example = "Drum, steel", maxLength = 100)
  private String description;

  @Schema(description = "Declaration of wood usage in packaging.\n- `NOT_APPLICABLE`\n- `NOT_TREATED_AND_NOT_CERTIFIED`\n- `PROCESSED`\n- `TREATED_AND_CERTIFIED`", example = "TREATED_AND_CERTIFIED", maxLength = 30)
  private String woodDeclaration;

  @Schema(description = "List of dangerous goods included in this packaging.")
  private List<DangerousGoods> dangerousGoods;
}
