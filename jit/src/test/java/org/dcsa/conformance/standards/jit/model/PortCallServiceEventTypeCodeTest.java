package org.dcsa.conformance.standards.jit.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PortCallServiceEventTypeCodeTest {

  @Test
  void testGetValidPortCallServiceTypes() {
    assertEquals(
        PortCallServiceEventTypeCode.getValidPortCallServiceTypes(
            PortCallServiceEventTypeCode.STRT),
        PortCallServiceEventTypeCode.getValidPortCallServiceTypes(
            PortCallServiceEventTypeCode.CMPL));
    assertTrue(
        PortCallServiceEventTypeCode.getValidPortCallServiceTypes(PortCallServiceEventTypeCode.STRT)
            .contains(PortCallServiceType.CARGO_OPERATIONS));
  }

  @Test
  void testGetCodesForPortCallServiceType() {
    assertEquals(
        List.of(PortCallServiceEventTypeCode.STRT, PortCallServiceEventTypeCode.CMPL),
        PortCallServiceEventTypeCode.getCodesForPortCallServiceType("CARGO_OPERATIONS"));
    assertTrue(
        PortCallServiceEventTypeCode.getCodesForPortCallServiceType("SAFETY")
            .contains(PortCallServiceEventTypeCode.DEPA));
  }

  @Test
  void testFromString() {
    assertEquals(
        PortCallServiceEventTypeCode.STRT, PortCallServiceEventTypeCode.fromString("STRT"));
  }
}
