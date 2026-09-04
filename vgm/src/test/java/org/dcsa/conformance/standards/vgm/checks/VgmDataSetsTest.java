package org.dcsa.conformance.standards.vgm.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VgmDataSetsTest {

  @Test
  void loadsWeightUnitValuesFromSchemaDescription() {
    assertTrue(VgmDataSets.VGM_WEIGHT_UNIT.contains("KGM"));
    assertTrue(VgmDataSets.VGM_WEIGHT_UNIT.contains("LBR"));
    assertFalse(VgmDataSets.VGM_WEIGHT_UNIT.contains("KG"));
  }

  @Test
  void loadsMethodValuesFromSchemaDescription() {
    assertTrue(VgmDataSets.VGM_METHOD.contains("SM1"));
    assertTrue(VgmDataSets.VGM_METHOD.contains("SM2"));
    assertFalse(VgmDataSets.VGM_METHOD.contains("METHOD1"));
  }
}

