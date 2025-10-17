package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;

@UtilityClass
public class EblInputPayloadValidations {

  public static Set<String> validateEblSchema(
      JsonNode bookingNode, JsonSchemaValidator schemaValidator) {
    return schemaValidator.validate(bookingNode);
  }

  public static Set<String> validateEblContent(
          JsonNode eblNode, ScenarioType scenarioType, boolean isTD) {
    List<JsonContentCheck> contentChecks = new ArrayList<>(EblChecks.STATIC_SI_CHECKS);
    contentChecks.add(EblChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE);
    contentChecks.add(EblChecks.VALIDATE_DOCUMENT_PARTIES_MATCH_EBL);
    contentChecks.addAll(EblChecks.generateScenarioRelatedChecks(scenarioType, isTD));
    return contentChecks.stream()
        .flatMap(check -> check.validate(eblNode).getErrorMessages().stream())
        .collect(Collectors.toSet());
  }
}
