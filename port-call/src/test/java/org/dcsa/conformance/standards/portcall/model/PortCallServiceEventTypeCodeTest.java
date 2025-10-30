package org.dcsa.conformance.standards.portcall.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PortCallServiceEventTypeCodeTest {

  @Test
  void testGetValidPortCallServiceTypeCodes() {
    assertEquals(
        PortCallServiceEventTypeCode.getValidPortCallServiceTypeCodes(
            PortCallServiceEventTypeCode.STRT),
        PortCallServiceEventTypeCode.getValidPortCallServiceTypeCodes(
            PortCallServiceEventTypeCode.CMPL));
    assertTrue(
        PortCallServiceEventTypeCode.getValidPortCallServiceTypeCodes(
                PortCallServiceEventTypeCode.STRT)
            .contains(PortCallServiceTypeCode.CARGO_OPERATIONS));
  }

  @Test
  void testGetCodesForPortCallServiceTypeCode() {
    assertEquals(
        List.of(PortCallServiceEventTypeCode.STRT, PortCallServiceEventTypeCode.CMPL),
        PortCallServiceEventTypeCode.getCodesForPortCallServiceTypeCode("CARGO_OPERATIONS"));
    assertTrue(
        PortCallServiceEventTypeCode.getCodesForPortCallServiceTypeCode("SAFETY")
            .contains(PortCallServiceEventTypeCode.DEPA));
  }

  @Test
  void testFromString() {
    assertEquals(
        PortCallServiceEventTypeCode.STRT, PortCallServiceEventTypeCode.fromString("STRT"));
  }
}
