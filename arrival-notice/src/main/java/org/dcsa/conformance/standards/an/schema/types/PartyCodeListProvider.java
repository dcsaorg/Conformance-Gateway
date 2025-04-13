package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.standards.an.schema.EnumBase;

@Schema(
    type = "string",
    example = "W3C",
    description =
"""
Code of the provider of the code list from which a party code is used.
""")
@AllArgsConstructor
public enum PartyCodeListProvider implements EnumBase {
  BOLE("Bolero"),
  CARX("CargoX"),
  DCSA("Digital Container Shipping Association"),
  DNB("Dun and Bradstreet"),
  EDOX("EdoxOnline"),
  ESSD("EssDOCS"),
  ETEU("eTEU"),
  FMC("Federal Maritime Commission"),
  GSBN("Global Shipping Business Network"),
  GLEIF("Global Legal Entity Identifier Foundation"),
  IDT("ICE Digital Trade"),
  IQAX("IQAX"),
  SECR("Secro"),
  TRGO("TradeGO"),
  W3C("World Wide Web Consortium"),
  WAVE("Wave"),
  WISE("WiseTech"),
  ZZZ("Mutually defined");

  @Getter private final String valueDescription;
}
