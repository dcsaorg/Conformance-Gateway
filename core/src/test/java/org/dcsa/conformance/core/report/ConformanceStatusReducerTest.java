package org.dcsa.conformance.core.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConformanceStatusReducerTest {

  @Test
  void reduce_nonConformantAndNonConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NON_CONFORMANT, ConformanceStatus.NON_CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_nonConformantAndPartiallyConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NON_CONFORMANT, ConformanceStatus.PARTIALLY_CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_nonConformantAndConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NON_CONFORMANT, ConformanceStatus.CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_nonConformantAndNoTraffic_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NON_CONFORMANT, ConformanceStatus.NO_TRAFFIC);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_partiallyConformantAndNonConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.PARTIALLY_CONFORMANT, ConformanceStatus.NON_CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_partiallyConformantAndPartiallyConformant_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.PARTIALLY_CONFORMANT, ConformanceStatus.PARTIALLY_CONFORMANT);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_partiallyConformantAndConformant_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.PARTIALLY_CONFORMANT, ConformanceStatus.CONFORMANT);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_partiallyConformantAndNoTraffic_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.PARTIALLY_CONFORMANT, ConformanceStatus.NO_TRAFFIC);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_conformantAndNonConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.CONFORMANT, ConformanceStatus.NON_CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_conformantAndPartiallyConformant_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.CONFORMANT, ConformanceStatus.PARTIALLY_CONFORMANT);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_conformantAndConformant_returnsConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(ConformanceStatus.CONFORMANT, ConformanceStatus.CONFORMANT);
    assertEquals(ConformanceStatus.CONFORMANT, result);
  }

  @Test
  void reduce_conformantAndNoTraffic_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(ConformanceStatus.CONFORMANT, ConformanceStatus.NO_TRAFFIC);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_noTrafficAndNonConformant_returnsNonConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NO_TRAFFIC, ConformanceStatus.NON_CONFORMANT);
    assertEquals(ConformanceStatus.NON_CONFORMANT, result);
  }

  @Test
  void reduce_noTrafficAndPartiallyConformant_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(
            ConformanceStatus.NO_TRAFFIC, ConformanceStatus.PARTIALLY_CONFORMANT);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_noTrafficAndConformant_returnsPartiallyConformant() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(ConformanceStatus.NO_TRAFFIC, ConformanceStatus.CONFORMANT);
    assertEquals(ConformanceStatus.PARTIALLY_CONFORMANT, result);
  }

  @Test
  void reduce_noTrafficAndNoTraffic_returnsNoTraffic() {
    ConformanceStatus result =
        ConformanceStatusReducer.reduce(ConformanceStatus.NO_TRAFFIC, ConformanceStatus.NO_TRAFFIC);
    assertEquals(ConformanceStatus.NO_TRAFFIC, result);
  }
}
