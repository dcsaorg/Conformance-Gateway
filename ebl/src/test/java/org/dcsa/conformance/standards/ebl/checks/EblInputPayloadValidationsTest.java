package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.dsp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.List;
import java.util.Set;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.junit.jupiter.api.Test;

class EblInputPayloadValidationsTest {
  @Test
  void inputValidationFiltersOnlyIrrelevantErrors() {
    var relevant =
        JsonAttribute.customValidator(
            "mixed",
            body ->
                ConformanceCheckResult.withRelevance(
                    Set.of(ConformanceError.irrelevant(), ConformanceError.error("invalid"))));
    try (var checks = mockStatic(EblChecks.class)) {
      checks
          .when(() -> EblChecks.transportDocumentCarrierContentChecks(ScenarioType.REGULAR_SWB))
          .thenReturn(List.of(relevant));
      assertEquals(
          Set.of("invalid"),
          EblInputPayloadValidations.validateEblContent(
              OBJECT_MAPPER.createObjectNode(),
              ScenarioType.REGULAR_SWB,
              true,
              dsp("REGULAR_SWB")));
    }
  }
}
