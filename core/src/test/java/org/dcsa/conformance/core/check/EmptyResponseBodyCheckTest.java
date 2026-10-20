package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmptyResponseBodyCheckTest {
  private final UUID exchangeUuid = UUID.randomUUID();

  @ParameterizedTest
  @ValueSource(ints = {202, 204, 205})
  void acceptsAnEmptyBodyForTheConfiguredStatus(int status) {
    var check = new EmptyResponseBodyCheck(role -> true, exchangeUuid, status);

    assertTrue(check.performCheck(id -> exchange(status, "")).isConformant());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "null", " ", "\n", "[]", "payload"})
  void rejectsEveryNonemptyBodyForTheConfiguredStatus(String body) {
    var check = new EmptyResponseBodyCheck(role -> true, exchangeUuid, 202);

    assertEquals(
        Set.of("A 202 response must not contain a payload."),
        check.performCheck(id -> exchange(202, body)).getErrorMessages());
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 201, 204, 400, 500})
  void leavesOtherResponseStatusesToTheirOwnChecks(int status) {
    var check = new EmptyResponseBodyCheck(role -> true, exchangeUuid, 202);

    assertTrue(check.performCheck(id -> exchange(status, "payload")).isConformant());
  }

  @Test
  void doesNotFailWhenTheExchangeIsNotAvailable() {
    var check = new EmptyResponseBodyCheck(role -> true, exchangeUuid, 202);

    assertTrue(
        check
            .performCheck(
                id -> {
                  assertEquals(exchangeUuid, id);
                  return null;
                })
            .isConformant());
  }

  @Test
  void preservesTheTitlePrefixAndRolePredicate() {
    var check = new EmptyResponseBodyCheck("[Notification]", "Carrier"::equals, exchangeUuid, 204);

    assertEquals("[Notification] The HTTP 204 response has no payload", check.getTitle());
    assertTrue(check.isRelevantForRole("Carrier"));
    assertFalse(check.isRelevantForRole("Shipper"));
    assertEquals(
        Set.of("A 204 response must not contain a payload."),
        check.performCheck(id -> exchange(204, "{}")).getErrorMessages());
  }

  private static ConformanceExchange exchange(int status, String body) {
    var request =
        new ConformanceRequest(
            "GET",
            "http://localhost/resource",
            Map.of(),
            new ConformanceMessage(
                "Shipper",
                "Shipper",
                "Carrier",
                "Carrier",
                Map.of(),
                new ConformanceMessageBody(""),
                0));
    return new ConformanceExchange(
        request, request.createResponse(status, Map.of(), new ConformanceMessageBody(body)));
  }
}
