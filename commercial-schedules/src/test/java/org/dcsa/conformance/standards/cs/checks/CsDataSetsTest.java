package org.dcsa.conformance.standards.cs.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CsDataSetsTest {

  @Test
  void loadsCutoffDateTimeCodeValuesFromSchemaDescription() {
    assertTrue(CsDataSets.CUTOFF_DATE_TIME_CODES.contains("DCO"));
    assertTrue(CsDataSets.CUTOFF_DATE_TIME_CODES.contains("AFC"));
    assertTrue(CsDataSets.CUTOFF_DATE_TIME_CODES.contains("RCO"));
    assertFalse(CsDataSets.CUTOFF_DATE_TIME_CODES.contains("XXX"));
  }
}

