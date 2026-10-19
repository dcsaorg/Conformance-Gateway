package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToStringComparatorTest {
  @Test
  void comparisonIsAntisymmetricAcrossJsonNodeTypes() throws Exception {
    List<JsonNode> nodes =
        List.of(
            OBJECT_MAPPER.readTree("{}"),
            OBJECT_MAPPER.readTree("[]"),
            OBJECT_MAPPER.readTree("{\"z\":[2,1],\"a\":true}"),
            OBJECT_MAPPER.readTree("{\"a\":false}"),
            OBJECT_MAPPER.readTree("[1,2]"),
            OBJECT_MAPPER.readTree("1"));
    for (JsonNode a : nodes) {
      for (JsonNode b : nodes) {
        assertEquals(
            -Integer.signum(ToStringComparator.INSTANCE.compare(b, a)),
            Integer.signum(ToStringComparator.INSTANCE.compare(a, b)));
      }
    }
  }
}
