package org.dcsa.conformance.end.action;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.end.EblEndorsementChainStandard;
import org.dcsa.conformance.end.EndorsementChainComponentFactory;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsumerGetEndorsementChainActionTest {

  private static final EndorsementChainComponentFactory COMPONENT_FACTORY =
    new EndorsementChainComponentFactory(
      "Ebl Endorsement Chain",
      "3.0.3",
      EblEndorsementChainStandard.SCENARIO_SUITE_CONFORMANCE);

  @ParameterizedTest
  @CsvSource({"199, false", "200, true", "202, true", "204, true", "299, true", "300, false"})
  void acceptsAny2xxResponseStatus(int responseStatus, boolean expectedConformant) {
    ConformanceExchange exchange = exchange(responseStatus);
    ConsumerGetEndorsementChainAction action =
      new ConsumerGetEndorsementChainAction(
        "Provider",
        "Consumer",
        null,
        COMPONENT_FACTORY.getMessageSchemaValidator("endorsementChains"),
        "GET EndorsementChain",
        SuppliedScenarioParameters.fromMap(
          Map.of(
            EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE,
            "HHL71800000")));
    action.handleExchange(exchange);

    ResponseStatusCheck statusCheck =
      allChecks(action.createCheck("3.0.3"))
        .filter(ResponseStatusCheck.class::isInstance)
        .map(ResponseStatusCheck.class::cast)
        .findFirst()
        .orElseThrow();
    statusCheck.check(ignored -> exchange);
    ConformanceResult result = statusCheck.resultsStream().findFirst().orElseThrow();

    assertEquals(expectedConformant, result.isConformant());
  }

  private static Stream<ConformanceCheck> allChecks(ConformanceCheck check) {
    return Stream.concat(Stream.of(check), check.subChecksStream().flatMap(ConsumerGetEndorsementChainActionTest::allChecks));
  }

  private static ConformanceExchange exchange(int responseStatus) {
    ConformanceRequest request =
      new ConformanceRequest(
        "GET",
        "http://localhost/endorsement-chains/HHL71800000",
        Map.of(),
        new ConformanceMessage(
          "Consumer",
          "Consumer",
          "Provider",
          "Provider",
          Map.of("API-Version", List.of("3.0.3")),
          new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()),
          System.currentTimeMillis()));
    return new ConformanceExchange(
      request,
      request.createResponse(
        responseStatus,
        Map.of("API-Version", List.of("3.0.3")),
        new ConformanceMessageBody(OBJECT_MAPPER.createArrayNode())));
  }
}

