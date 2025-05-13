package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.CountryCode;
import org.dcsa.conformance.specifications.an.v100.types.CustomsReferenceValue;

@Data
@Schema(
    description =
"""
Reference associated with customs and/or excise purposes required by the relevant authorities for the import, export
or transit of the goods.

Examples of possible customs references are:

| Type  | Country | Description |
|-------|:-------:|-------------|
|UCR|NL|Unique Consignment Reference|
|CUS|NL|Customs Union and Statistics|
|ACID|EG|Advance Cargo Information Declaration in Egypt|
|CERS|CA|Canadian Export Reporting System|
|ITN|US|Internal Transaction Number in US|
|PEB|ID|PEB reference number|
|CSN|IN|Cargo Summary Notification (CSN)|
""")
public class CustomsReference {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      example = "CUS",
      description = "The reference type code as defined in the relevant customs jurisdiction.")
  private String typeCode;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private CountryCode countryCode;

  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    description = "List of customs reference values")
  @ArraySchema(minItems = 1)
  private List<CustomsReferenceValue> referenceValues;
}
