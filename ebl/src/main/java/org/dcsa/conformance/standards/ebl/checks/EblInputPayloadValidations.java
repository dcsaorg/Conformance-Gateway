package org.dcsa.conformance.standards.ebl.checks;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.ConformanceErrorSeverity;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;
@UtilityClass
public class EblInputPayloadValidations {
  public static Set<String> validateEblSchema(
      JsonNode bookingNode, JsonSchemaValidator schemaValidator) {
    return schemaValidator.validate(bookingNode);
  }
  public static Set<String> validateEblContent(
      JsonNode eblNode, ScenarioType scenarioType, boolean isTD, EblDynamicScenarioParameters dsp) {
    List<JsonContentCheck> contentChecks;
    if (isTD) {
      contentChecks = EblChecks.transportDocumentCarrierContentChecks(scenarioType);
    } else {
      contentChecks = new ArrayList<>(EblChecks.STATIC_SI_CHECKS);
      contentChecks.addAll(
          EblChecks.generateScenarioRelatedChecks(scenarioType, false, dsp.isCladInSI()));
    }
    return contentChecks.stream()
        .filter(JsonContentCheck::isRelevant)
        .flatMap(
            check -> {
              ConformanceCheckResult result = check.validate(eblNode);
              return switch (result) {
                case ConformanceCheckResult.SimpleErrors(var errors) -> errors.stream();
                case ConformanceCheckResult.ErrorsWithRelevance(var errors) ->
                    errors.stream()
                        .filter(
                            conformanceError ->
                                !ConformanceErrorSeverity.IRRELEVANT.equals(
                                    conformanceError.severity()))
                        .map(ConformanceError::message);
              };
            })
        .collect(Collectors.toSet());
  }
}
