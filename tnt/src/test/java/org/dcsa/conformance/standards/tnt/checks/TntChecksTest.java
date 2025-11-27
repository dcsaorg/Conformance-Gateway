package org.dcsa.conformance.standards.tnt.checks;

import static org.dcsa.conformance.standards.tnt.checks.TntChecks.VALIDATE_NON_EMPTY_EVENTS;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TntChecksTest {

  private ObjectMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
  }

  @Test
  void testValidateNonEmptyEvents_noEventsFound() {

    ArrayNode emptyBody = mapper.createArrayNode();

    Set<String> errors = VALIDATE_NON_EMPTY_EVENTS.validate(emptyBody).getErrorMessages();

    assertFalse(errors.isEmpty());
  }

  @Test
  void testValidateNonEmptyEvents_withEvents() {

    ArrayNode bodyWithEvents = mapper.createArrayNode();

    ObjectNode obj = bodyWithEvents.addObject();
    ArrayNode events = obj.putArray("events");
    events.addObject().put("eventType", "SHIPMENT");

    Set<String> errors = VALIDATE_NON_EMPTY_EVENTS.validate(bodyWithEvents).getErrorMessages();

    assertTrue(errors.isEmpty());
  }
}
