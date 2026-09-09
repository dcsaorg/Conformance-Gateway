package org.dcsa.conformance.end.checks;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class EndChainDataSets {

  static final Set<String> VALID_ACTION_CODES =
    Set.of(
      "ISSUE",
      "ENDORSE",
      "SIGN",
      "SURRENDER_FOR_DELIVERY",
      "SURRENDER_FOR_AMENDMENT",
      "BLANK_ENDORSE",
      "ENDORSE_TO_ORDER",
      "TRANSFER",
      "SURRENDERED");

  static final Set<String> VALID_EBL_PLATFORMS =
    Set.of(
      "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU",
      "TRAC", "BRIT", "COVA", "ETIT", "KTNE", "CRED", "BLOC", "DOCU", "AEOT", "SGTD");

  static final Set<String> VALID_CODE_LIST_PROVIDERS =
    Set.of(
      "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU",
      "TRAC", "BRIT", "COVA", "ETIT", "KTNE", "CRED", "BLOC", "DOCU", "AEOT", "SGTD",
      "GSBN", "WISE", "GLEIF", "W3C", "DNB", "FMC", "DCSA", "ZZZ");
}
