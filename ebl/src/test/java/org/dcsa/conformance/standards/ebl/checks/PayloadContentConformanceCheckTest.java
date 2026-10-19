package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.exchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.junit.jupiter.api.Test;

class PayloadContentConformanceCheckTest {
  @Test
  void payloadSubchecksDelegateOnlyNonemptyContentForBothMessageDirections() {
    JsonContentCheck validator = JsonAttribute.mustEqual(JsonPointer.compile("/value"), "ok");
    for (HttpMessageType direction : HttpMessageType.values()) {
      var check =
          new PayloadContentConformanceCheck(role -> true, UUID.randomUUID(), direction) {
            @Override
            protected Stream<? extends ConformanceCheck> createSubChecks() {
              return buildChecks("payload", "/nested", () -> List.of(validator));
            }
          };
      assertTrue(check.performCheck(id -> null).isConformant());
      var sub = (ActionCheck) check.createSubChecks().findFirst().orElseThrow();
      sub.check(id -> null);
      ObjectNode payload = OBJECT_MAPPER.createObjectNode();
      for (int state = 0; state < 4; state++) {
        if (state == 1) payload.putObject("nested");
        if (state == 2) payload.withObject("/nested").put("value", "bad");
        if (state == 3) payload.withObject("/nested").put("value", "ok");
        var exchange = exchange(payload, payload);
        sub.check(id -> exchange);
        assertEquals(
            state != 2, sub.resultsStream().reduce((a, b) -> b).orElseThrow().isConformant());
      }
      assertTrue(sub.isRelevantForRole("Carrier"));
      assertEquals(4, sub.resultsStream().count());
      assertEquals(1, sub.resultsStream().filter(result -> !result.isConformant()).count());
    }
  }
}
