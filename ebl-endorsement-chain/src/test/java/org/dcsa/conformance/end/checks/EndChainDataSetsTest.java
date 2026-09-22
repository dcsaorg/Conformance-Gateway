package org.dcsa.conformance.end.checks;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndChainDataSetsTest {

  @Test
  void actionCodeDataSetMatchesConformanceDocument() {
    assertEquals(
      Set.of(
        "ISSUE",
        "ENDORSE",
        "SIGN",
        "SURRENDER_FOR_DELIVERY",
        "SURRENDER_FOR_AMENDMENT",
        "BLANK_ENDORSE",
        "ENDORSE_TO_ORDER",
        "TRANSFER",
        "SURRENDERED"),
      EndChainDataSets.VALID_ACTION_CODES);
  }

  @Test
  void eblPlatformDataSetMatches303Schema() {
    assertEquals(
      Set.of(
        "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU",
        "TRAC", "BRIT", "COVA", "ETIT", "KTNE", "CRED", "BLOC", "DOCU", "AEOT", "SGTD"),
      EndChainDataSets.VALID_EBL_PLATFORMS);
  }

  @Test
  void codeListProviderDataSetMatches303Schema() {
    assertEquals(
      Set.of(
        "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU",
        "TRAC", "BRIT", "COVA", "ETIT", "KTNE", "CRED", "BLOC", "DOCU", "AEOT", "SGTD",
        "GSBN", "WISE", "GLEIF", "W3C", "DNB", "FMC", "DCSA", "ZZZ"),
      EndChainDataSets.VALID_CODE_LIST_PROVIDERS);
  }
}

