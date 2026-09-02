package org.dcsa.conformance.standards.tnt.v300.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.tnt.v300.party.TntConstants;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplyScenarioParametersActionTest {

  private static final TntQueryParameters[] BASE_FILTERS = {
    TntQueryParameters.CBR,
    TntQueryParameters.TDR,
    TntQueryParameters.ER
  };

  @Test
  void optionalOnlyActionRequiresAtLeastOneParameter() {
    SupplyScenarioParametersAction action = optionalBaseFiltersAction();
    ObjectNode partyInput = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    partyInput.putObject(TntConstants.INPUT);

    UserFacingException exception =
      assertThrows(UserFacingException.class, () -> action.handlePartyInput(partyInput));

    assertTrue(exception.getMessage().contains("at least one"));
    for (TntQueryParameters parameter : BASE_FILTERS) {
      assertTrue(exception.getMessage().contains(parameter.getParameterName()));
    }
  }

  @ParameterizedTest
  @MethodSource("baseFilters")
  void optionalOnlyActionAcceptsEachParameterIndividually(TntQueryParameters parameter) {
    SupplyScenarioParametersAction action = optionalBaseFiltersAction();
    ObjectNode partyInput = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    partyInput.putObject(TntConstants.INPUT).put(parameter.getParameterName(), "example-value");

    assertDoesNotThrow(() -> action.handlePartyInput(partyInput));

    assertEquals(
      "example-value",
      action.getSuppliedScenarioParameters().getMap().get(parameter));
  }

  @Test
  void optionalPromptExplainsAtLeastOneParameterIsRequired() {
    String prompt = optionalBaseFiltersAction().getHumanReadablePrompt();

    assertTrue(prompt.contains("at least one"));
    assertTrue(prompt.contains("one or more"));
  }

  private static Stream<TntQueryParameters> baseFilters() {
    return Stream.of(BASE_FILTERS);
  }

  private static SupplyScenarioParametersAction optionalBaseFiltersAction() {
    return SupplyScenarioParametersAction.optional("producer", BASE_FILTERS);
  }
}

