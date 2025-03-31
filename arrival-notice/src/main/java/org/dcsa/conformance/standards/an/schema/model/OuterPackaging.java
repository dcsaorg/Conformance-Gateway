package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = """
Outer packaging / overpack specification.
 Examples of overpacks are a number of packages stacked on to a pallet and secured by strapping
 or placed in a protective outer packaging such as a box or crate to form one unit for the convenience
 of handling and stowage during transport.""")
public class OuterPackaging {

  @Schema(
      description =
          """
A code identifying the outer packaging / overpack.
 PackageCode must follow the codes specified in Recommendation N°21:
 https://unece.org/trade/uncefact/cl-recommendations

Only applicable to dangerous goods if the IMO packaging code is not available.""",
      example = "5H")
  private String packageCode;

  @Schema(
    description =
      """
The code of the packaging as per IMO.
 Only applicable to dangerous goods if specified in the IMO IMDG code:
 https://www.imo.org/en/publications/Pages/IMDG%20Code.aspx

If not available, the packageCode as per UN recommendation 21 should be used.""",
    example = "1A2")
  private String imoPackagingCode;

  @Schema(description = "Specifies the number of outer packagings / overpacks associated with this Cargo Item.", example = "18")
  private String numberOfPackages;

  @Schema(description = "Description of the outer packaging/overpack.", example = "Drum, steel")
  private String description;

  @Schema(
      description =
          """
Property to clearly indicate if the products, packaging and any other items are made of wood. Possible values include:
 * NOT_APPLICABLE (if no wood or any other wood product such as packaging and supports are being shipped)
 * NOT_TREATED_AND_NOT_CERTIFIED (if the wood or wooden materials have not been treated nor fumigated and do not
  include a certificate)
 * PROCESSED (if the wood or wooden materials are entirely made of processed wood, such as plywood, particle board,
  sliver plates of wood and wood laminate sheets produced using glue, heat, pressure or a combination of these)
 * TREATED_AND_CERTIFIED (if the wood or wooden materials have been treated and/or fumigated and include a certificate)
""",
      example = "TREATED_AND_CERTIFIED")
  private String woodDeclaration;

  @Schema(description = "List of Dangerous Goods.")
  private List<DangerousGoods> dangerousGoods;
}
