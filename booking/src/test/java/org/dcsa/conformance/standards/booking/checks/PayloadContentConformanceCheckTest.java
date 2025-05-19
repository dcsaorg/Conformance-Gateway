package org.dcsa.conformance.standards.booking.checks;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PayloadContentConformanceCheckTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  private PayloadContentConformanceCheck check;

  @BeforeEach
  void setup() {
    check =
        new PayloadContentConformanceCheck(
            "test", BookingRole::isShipper, UUID.randomUUID(), HttpMessageType.REQUEST) {
          @Override
          protected Stream<? extends ConformanceCheck> createSubChecks() {
            return Stream.empty();
          }
        };
  }

  @Test
  void shouldSkipWhenNodeIsMissing() throws Exception {
    Function<JsonNode, Set<String>> subCheck = Mockito.mock(Function.class);
    var fn = check.at("/nonexistent", subCheck);

    JsonNode json = mapper.readTree("{\"someField\":\"value\"}");
    Set<String> result = fn.apply(json);

    assertEquals(Collections.emptySet(), result);
    verifyNoInteractions(subCheck);
  }

  @Test
  void shouldSkipWhenNodeIsNull() throws Exception {
    Function<JsonNode, Set<String>> subCheck = Mockito.mock(Function.class);
    var fn = check.at("/nullField", subCheck);

    JsonNode json = mapper.readTree("{\"nullField\": null}");
    Set<String> result = fn.apply(json);

    assertEquals(Collections.emptySet(), result);
    verifyNoInteractions(subCheck);
  }

  @Test
  void shouldInvokeSubCheckWhenNodeIsValid() throws Exception {
    Function<JsonNode, Set<String>> subCheck = Mockito.mock(Function.class);
    var fn = check.at("/name", subCheck);

    JsonNode json = mapper.readTree("{\"name\": {\"firstName\":\"value\"}}");
    when(subCheck.apply(any())).thenReturn(Set.of("test"));

    Set<String> result = fn.apply(json);

    assertEquals(Set.of("test"), result);
    verify(subCheck, times(1)).apply(json.get("name"));
  }
}
