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
Reference associated with customs and/or excise purposes required by the relevant authorities for the import, export,
 or transit of the goods.
""")
public class CustomsReference {

  @Schema(
      description = "The reference type code as defined in the relevant customs jurisdiction.",
      example = "CUS")
  private String crType;

  @Schema(
      description =
"""
The 2 characters for the country code using ISO 3166-1 alpha-2
""",
      example = "NL")
  private String countryCode;

  @Schema(
      description =
"""
A list of national commodity codes values
""",
      example = "[1234, 5678]")
  private List<String> values;
}
