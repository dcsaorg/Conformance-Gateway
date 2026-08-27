package org.dcsa.conformance.standards.tnt.v300.checks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TntDataSetsTest {

  @Test
  void loadsEventClassifierValuesFromSchemaDescription() {
    assertTrue(TntDataSets.VALID_EVENT_CLASSIFIERS.contains("ACTUAL"));
    assertTrue(TntDataSets.VALID_EVENT_CLASSIFIERS.contains("ESTIMATED"));
    assertTrue(TntDataSets.VALID_EVENT_CLASSIFIERS.contains("PLANNED"));
  }

  @Test
  void loadsSubtypeValuesFromSchemaDescription() {
    assertTrue(TntDataSets.VALID_SHIPMENT_EVENT_TYPES.contains("APPROVED"));
    assertTrue(TntDataSets.VALID_TRANSPORT_EVENT_TYPES.contains("ARRIVED"));
    assertTrue(TntDataSets.VALID_EQUIPMENT_EVENT_TYPES.contains("LOADED"));
    assertTrue(TntDataSets.VALID_IOT_EVENT_TYPES.contains("DEVICE_STATUS"));
    assertTrue(TntDataSets.VALID_REEFER_EVENT_TYPES.contains("MEASURED"));
  }

  @Test
  void loadsDocumentReferenceTypeValuesFromSchemaDescription() {
    assertTrue(TntDataSets.VALID_DOCUMENT_REFERENCE_TYPES.contains("BOOKING"));
    assertTrue(TntDataSets.VALID_DOCUMENT_REFERENCE_TYPES.contains("TRANSPORT_DOCUMENT"));
    assertFalse(TntDataSets.VALID_DOCUMENT_REFERENCE_TYPES.contains("NOT_A_REAL_TYPE"));
  }
}

