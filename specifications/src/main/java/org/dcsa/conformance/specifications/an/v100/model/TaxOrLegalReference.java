package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.types.CountryCode;

@Data
@Schema(
    description =
"""
Reference that uniquely identifies a party for tax and/or legal purposes in accordance with the relevant jurisdiction.

This is a limited list of examples:

| Type  | Country | Description |
|-------|:-------:|-------------|
|EORI|NL|Economic Operators Registration and Identification|
|PAN|IN|Goods and Services Tax Identification Number in India|
|GSTIN|IN|Goods and Services Tax Identification Number in India|
|IEC|IN|Importer-Exported Code in India|
|RUC|EC|Registro Único del Contribuyente in Ecuador|
|RUC|PE|Registro Único del Contribuyente in Peru|
|NIF|MG|Numéro d'Identification Fiscal in Madagascar|
|NIF|DZ|Numéro d'Identification Fiscal in Algeria|
""")
public class TaxOrLegalReference {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50,
      example = "PAN",
      description =
          "The reference type code as defined by the relevant tax and/or legal authority.")
  private String referenceType;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private CountryCode countryCode;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 35,
      example = "ABC1234",
      description = "The name or title of the tax or legal reference")
  private String value;
}
