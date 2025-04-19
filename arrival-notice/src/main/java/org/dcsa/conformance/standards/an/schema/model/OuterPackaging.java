package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.standards.an.schema.SchemaOverride;
import org.dcsa.conformance.standards.an.schema.types.ImoPackagingCode;
import org.dcsa.conformance.standards.an.schema.types.UnecePackageCode;
import org.dcsa.conformance.standards.an.schema.types.WoodDeclarationTypeCode;

@Data
@Schema(
    description =
"""
Outer packaging / overpack specification.

 Examples of overpacks are a number of packages stacked on to a pallet and secured by strapping
 or placed in a protective outer packaging such as a box or crate to form one unit
 for the convenience of handling and stowage during transport.
""")
public class OuterPackaging {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 100,
      example = "Drum, steel",
      description = "Description of the outer packaging / overpack")
  private String description;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "integer",
      format = "int32",
      minimum = "1",
      maximum = "99999999",
      example = "123",
      description =
          "Specifies the number of outer packagings / overpacks associated with this Cargo Item.")
  private String numberOfPackages;

  @SchemaOverride(
      description =
"""
IMO packaging code.

Only applicable to dangerous goods if specified in the
 [IMO IMDG code](https://www.imo.org/en/publications/Pages/IMDG%20Code.aspx).

If not available, the UNECE Recommendation 21 `packageCode` should be used.
""")
  private ImoPackagingCode imoPackagingCode;

  @Schema(
      description =
"""
[UNECE Recommendation 21](https://unece.org/trade/uncefact/cl-recommendations) package code

Only applicable to dangerous goods if the `imoPackagingCode` is not available.""",
      example = "5H")
  private UnecePackageCode unecePackageCode;

  @Schema()
  private WoodDeclarationTypeCode woodDeclaration;

  @Schema(description = "List of dangerous goods specifications")
  private List<DangerousGoods> dangerousGoods;
}
