package org.dcsa.conformance.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapUtilsTest {

  @Test
  void mergePartyScenarioModulesPreservesUniqueLabelsAndQualifiesCollisions() {
    Object carrierRequired = new Object();
    Object carrierOptional = new Object();
    Object shipperRequired = new Object();
    Object shipperUnique = new Object();
    Map<String, Map<String, Object>> modulesByRole =
        MapUtils.orderedMap(
            Map.entry(
                "Carrier",
                MapUtils.orderedMap(
                    Map.entry("Required", carrierRequired),
                    Map.entry("Carrier only", carrierOptional))),
            Map.entry(
                "Shipper",
                MapUtils.orderedMap(
                    Map.entry("Required", shipperRequired),
                    Map.entry("Shipper only", shipperUnique))));

    Map<String, Object> merged =
        MapUtils.mergePartyScenarioModules(modulesByRole, List.of("Carrier", "Shipper"));

    assertEquals(
        List.of("Carrier: Required", "Carrier only", "Shipper: Required", "Shipper only"),
        List.copyOf(merged.keySet()));
    assertSame(carrierRequired, merged.get("Carrier: Required"));
    assertSame(shipperRequired, merged.get("Shipper: Required"));
  }

  @Test
  void mergePartyScenarioModulesKeepsSingleRoleLabelsUnchanged() {
    Object required = new Object();
    Map<String, Map<String, Object>> modulesByRole =
        Map.of("Carrier", Map.of("Required", required));

    Map<String, Object> merged =
        MapUtils.mergePartyScenarioModules(modulesByRole, List.of("Carrier"));

    assertEquals(List.of("Required"), List.copyOf(merged.keySet()));
    assertSame(required, merged.get("Required"));
  }
}

