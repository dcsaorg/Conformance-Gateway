package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Reference associated with customs and/or excise purposes required by relevant authorities for the import, export, or transit of goods.\n\nExamples include:\n- UCR (Unique Consignment Reference)\n- ACID (Advance Cargo Info Declaration in Egypt)\n- ITN (Internal Transaction Number in the US)")
@Data
public class CustomsReference {

  @Schema(description = "The reference type code as defined in the relevant customs jurisdiction.", example = "CUS", maxLength = 50, pattern = "^\\S(?:.*\\S)?$")
  private String type;

  @Schema(description = "The 2-character country code using ISO 3166-1 alpha-2.", example = "NL", minLength = 2, maxLength = 2, pattern = "^[A-Z]{2}$")
  private String countryCode;

  @ArraySchema(
    schema = @Schema(description = "The value(s) of the customs reference.", example = "4988470982020120017", maxLength = 35, pattern = "^\\S(?:.*\\S)?$"),
    minItems = 1
  )
  private List<String> values;
}
