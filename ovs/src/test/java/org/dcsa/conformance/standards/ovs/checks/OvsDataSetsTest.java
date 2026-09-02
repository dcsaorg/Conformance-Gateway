package org.dcsa.conformance.standards.ovs.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OvsDataSetsTest {

  @Test
  void loadsStatusCodeValuesFromSchemaDescription() {
    assertTrue(OVSDataSets.STATUS_CODE.contains("OMIT"));
    assertTrue(OVSDataSets.STATUS_CODE.contains("CUTR"));
    assertFalse(OVSDataSets.STATUS_CODE.contains("DRYD"));
  }

  @Test
  void loadsStatusCodesValuesFromSchemaDescription() {
    assertTrue(OVSDataSets.STATUS_CODES.contains("OMIT"));
    assertTrue(OVSDataSets.STATUS_CODES.contains("DRYD"));
    assertTrue(OVSDataSets.STATUS_CODES.contains("OOSV"));
    assertFalse(OVSDataSets.STATUS_CODES.contains("UNKNOWN"));
  }
}

