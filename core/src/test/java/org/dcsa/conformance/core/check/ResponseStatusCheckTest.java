package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResponseStatusCheckTest {

  private final UUID exchangeUuid = UUID.randomUUID();

  @ParameterizedTest
  @ValueSource(ints = {200, 201, 202, 204, 226, 299})
  void successfulResponseCheckAcceptsEvery2xxStatus(int status) {
    var check = ResponseStatusCheck.forSuccessfulResponse(role -> true, exchangeUuid);

    assertTrue(check.performCheck(ignored -> exchange(status)).isConformant());
  }

  @ParameterizedTest
  @ValueSource(ints = {199, 300, 400, 500})
  void successfulResponseCheckRejectsStatusesOutside2xx(int status) {
    var check = ResponseStatusCheck.forSuccessfulResponse(role -> true, exchangeUuid);

    assertFalse(check.performCheck(ignored -> exchange(status)).isConformant());
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 202, 204, 299})
  void exactGetStyleCheckStillRejectsOther2xxStatuses(int status) {
    var check = new ResponseStatusCheck(role -> true, exchangeUuid, 200);

    assertFalse(check.performCheck(ignored -> exchange(status)).isConformant());
  }

  private static ConformanceExchange exchange(int status) {
    ConformanceExchange exchange = mock(ConformanceExchange.class);
    ConformanceResponse response = mock(ConformanceResponse.class);
    when(exchange.getResponse()).thenReturn(response);
    when(response.statusCode()).thenReturn(status);
    return exchange;
  }
}

