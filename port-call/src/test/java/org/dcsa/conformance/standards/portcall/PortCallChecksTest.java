package org.dcsa.conformance.standards.portcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.portcall.checks.PortCallChecks;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortCallChecksTest {

  private ObjectMapper mapper;
  private ObjectNode body;
  private ObjectNode event;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    body = mapper.createObjectNode();
    ArrayNode events = body.putArray("events");
    event = events.addObject();
  }

  private static boolean isOk(JsonContentCheck check, ObjectNode body) {
    return check.validate(body).getErrorMessages().isEmpty();
  }

  private static boolean isFail(JsonContentCheck check, ObjectNode body) {
    return !isOk(check, body);
  }

  private ObjectNode addEvent() {
    return ((ArrayNode) body.get("events")).addObject();
  }

  private ObjectNode addMovesForecasts(ObjectNode event) {
    ArrayNode mfArr = event.putArray("movesForecasts");
    return mfArr.addObject();
  }

  @Nested
  class NonEmptyEventsTest {
    @Test
    void nonEmptyEvents_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(isFail(PortCallChecks.nonEmptyEvents(), bodyWithoutEvents));
    }

    @Test
    void nonEmptyEvents_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(isFail(PortCallChecks.nonEmptyEvents(), bodyWithEmptyEvents));
    }

    @Test
    void nonEmptyEvents_passes_whenAtLeastOneEventIsPresent() {
      assertTrue(isOk(PortCallChecks.nonEmptyEvents(), body));
    }
  }

  @Nested
  class AtLeastOneEventIncludesTimestampObjectTest {
    @Test
    void atLeastOneEventIncludesTimestampObject_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(
        isFail(PortCallChecks.atLeastOneEventIncludesTimestampObject(), bodyWithoutEvents));
    }

    @Test
    void atLeastOneEventIncludesTimestampObject_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(
        isFail(PortCallChecks.atLeastOneEventIncludesTimestampObject(), bodyWithEmptyEvents));
    }

    @Test
    void atLeastOneEventIncludesTimestampObject_fails_whenNoEventHasTimestamp() {
      addEvent();
      assertTrue(isFail(PortCallChecks.atLeastOneEventIncludesTimestampObject(), body));
    }

    @Test
    void atLeastOneEventIncludesTimestampObject_passes_whenFirstEventHasTimestamp() {
      event.putObject("timestamp").put("classifierCode", "EST");
      addEvent();
      assertTrue(isOk(PortCallChecks.atLeastOneEventIncludesTimestampObject(), body));
    }

    @Test
    void atLeastOneEventIncludesTimestampObject_passes_whenSecondEventHasTimestamp() {
      addEvent().putObject("timestamp").put("classifierCode", "EST");
      assertTrue(isOk(PortCallChecks.atLeastOneEventIncludesTimestampObject(), body));
    }
  }

  @Nested
  class AtLeastOneTimestampClassifierCodeCorrectTest {
    @Test
    void timestampClassifier_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(
        isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), bodyWithoutEvents));
    }

    @Test
    void timestampClassifier_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(
        isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), bodyWithEmptyEvents));
    }

    @Test
    void timestampClassifier_fails_whenMissingTimestamp() {
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
    }

    @Test
    void timestampClassifier_fails_whenClassifierCodeBlank() {
      event.putObject("timestamp").put("classifierCode", "");
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
    }

    @Test
    void timestampClassifier_passes_whenClassifierCodePresent() {
      event.putObject("timestamp").put("classifierCode", "EST");
      assertTrue(isOk(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
    }

    @Test
    void timestampClassifier_passes_whenSecondOfTwoEventsIsValid() {
      addEvent().putObject("timestamp").put("classifierCode", "EST");
      assertTrue(isOk(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
    }

    @Test
    void timestampClassifier_fails_whenAllEventsAreInvalid() {
      addEvent();
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
    }
  }

  @Nested
  class AtLeastOneTimestampServiceDateTimeCorrectTest {
    @Test
    void timestampServiceDateTime_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(
        isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), bodyWithoutEvents));
    }

    @Test
    void timestampServiceDateTime_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(
        isFail(
          PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), bodyWithEmptyEvents));
    }

    @Test
    void timestampServiceDateTime_fails_whenMissingTimestamp() {
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
    }

    @Test
    void timestampServiceDateTime_fails_whenBlank() {
      event.putObject("timestamp").put("serviceDateTime", "");
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
    }

    @Test
    void timestampServiceDateTime_passes_whenPresent() {
      event.putObject("timestamp").put("serviceDateTime", "2025-01-23T10:00:00Z");
      assertTrue(isOk(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
    }

    @Test
    void timestampServiceDateTime_passes_whenSecondOfTwoEventsIsValid() {
      addEvent().putObject("timestamp").put("serviceDateTime", "2025-01-23T10:00:00Z");
      assertTrue(isOk(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
    }

    @Test
    void timestampServiceDateTime_fails_whenAllEventsAreInvalid() {
      addEvent();
      assertTrue(isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
    }
  }

  @Nested
  class MovesForecastsArrayNonEmptyCheckTest {
    @Test
    void movesForecastsArrayNonEmpty_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(isFail(PortCallChecks.movesForecastsArrayNonEmptyCheck(), bodyWithoutEvents));
    }

    @Test
    void movesForecastsArrayNonEmpty_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(
        isFail(PortCallChecks.movesForecastsArrayNonEmptyCheck(), bodyWithEmptyEvents));
    }

    @Test
    void movesForecastsArrayNonEmpty_fails_whenMovesForecastsIsMissing() {
      assertTrue(isFail(PortCallChecks.movesForecastsArrayNonEmptyCheck(), body));
    }

    @Test
    void movesForecastsArrayNonEmpty_fails_whenMovesForecastsIsEmpty() {
      event.putArray("movesForecasts");
      assertTrue(isFail(PortCallChecks.movesForecastsArrayNonEmptyCheck(), body));
    }

    @Test
    void movesForecastsArrayNonEmpty_passes_whenNonEmpty() {
      addMovesForecasts(event);
      assertTrue(isOk(PortCallChecks.movesForecastsArrayNonEmptyCheck(), body));
    }
  }

  @Nested
  class MovesForecastsItemHasUnitsObjectCheckTest {
    @Test
    void movesForecastsItemHasUnitsObject_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(
        isFail(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), bodyWithoutEvents));
    }

    @Test
    void movesForecastsItemHasUnitsObject_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(
        isFail(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), bodyWithEmptyEvents));
    }

    @Test
    void movesForecastsItemHasUnitsObject_fails_whenMovesForecastsMissing() {
      assertTrue(isFail(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }

    @Test
    void movesForecastsItemHasUnitsObject_fails_whenMovesForecastsEmpty() {
      event.putArray("movesForecasts");
      assertTrue(isFail(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }

    @Test
    void movesForecastsItemHasUnitsObject_fails_whenItemHasNoUnitsObjects() {
      addMovesForecasts(event);
      assertTrue(isFail(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }

    @Test
    void movesForecastsItemHasUnitsObject_passes_whenRestowUnitsPresent() {
      addMovesForecasts(event).putObject("restowUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }

    @Test
    void movesForecastsItemHasUnitsObject_passes_whenLoadUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }

    @Test
    void movesForecastsItemHasUnitsObject_passes_whenDischargeUnitsPresent() {
      addMovesForecasts(event).putObject("dischargeUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.movesForecastsItemHasUnitsObjectCheck(), body));
    }
  }

  @Nested
  class LoadUnitsCategoryCheckTest {
    @Test
    void loadUnitsCategory_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(isFail(PortCallChecks.loadUnitsCategoryCheck(), bodyWithoutEvents));
    }

    @Test
    void loadUnitsCategory_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(isFail(PortCallChecks.loadUnitsCategoryCheck(), bodyWithEmptyEvents));
    }

    @Test
    void loadUnitsCategory_passes_whenMovesForecastsMissing() {
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenMovesForecastsEmpty() {
      event.putArray("movesForecasts");
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenLoadUnitsAbsentFromItem() {
      addMovesForecasts(event).putObject("restowUnits");
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenLoadUnitsIsExplicitNull() {
      addMovesForecasts(event).putNull("loadUnits");
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_fails_whenLoadUnitsIsEmpty() {
      addMovesForecasts(event).putObject("loadUnits");
      assertTrue(isFail(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenTotalUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 10);
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenLadenUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("ladenUnits", 4);
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenEmptyUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("emptyUnits", 4);
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenPluggedReeferUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("pluggedReeferUnits", 4);
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenOutOfGaugeUnitsPresent() {
      addMovesForecasts(event).putObject("loadUnits").put("outOfGaugeUnits", 4);
      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_fails_whenOneOfTwoOccurrencesIsInvalid() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 5);
      addMovesForecasts(event).putObject("loadUnits"); // invalid: empty

      assertTrue(isFail(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void loadUnitsCategory_passes_whenBothOccurrencesAreValid() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 5);
      addMovesForecasts(event).putObject("loadUnits").put("ladenUnits", 3);

      assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }
  }

  @Nested
  class DischargeUnitsCategoryCheckTest {
    @Test
    void dischargeUnitsCategory_passes_whenDischargeUnitsAbsentFromItem() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.dischargeUnitsCategoryCheck(), body));
    }

    @Test
    void dischargeUnitsCategory_fails_whenPresentButEmpty() {
      addMovesForecasts(event).putObject("dischargeUnits");
      assertTrue(isFail(PortCallChecks.dischargeUnitsCategoryCheck(), body));
    }

    @Test
    void dischargeUnitsCategory_passes_whenLadenUnitsPresent() {
      addMovesForecasts(event).putObject("dischargeUnits").put("ladenUnits", 10);
      assertTrue(isOk(PortCallChecks.dischargeUnitsCategoryCheck(), body));
    }
  }

  @Nested
  class RestowUnitsSizeCheckTest {
    @Test
    void restowUnitsSize_fails_whenEventsFieldIsMissing() {
      ObjectNode bodyWithoutEvents = mapper.createObjectNode();
      assertTrue(isFail(PortCallChecks.restowUnitsSizeCheck(), bodyWithoutEvents));
    }

    @Test
    void restowUnitsSize_fails_whenEventsArrayIsEmpty() {
      ObjectNode bodyWithEmptyEvents = mapper.createObjectNode();
      bodyWithEmptyEvents.putArray("events");
      assertTrue(isFail(PortCallChecks.restowUnitsSizeCheck(), bodyWithEmptyEvents));
    }

    @Test
    void restowUnitsSize_passes_whenMovesForecastsNotArray() {
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenRestowUnitsAbsent() {
      addMovesForecasts(event).putObject("loadUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenRestowUnitsIsExplicitNull() {
      addMovesForecasts(event).putNull("restowUnits");
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_fails_whenAllSizeAttributesAreInvalid() {
      ObjectNode mf = addMovesForecasts(event);
      ObjectNode restow = mf.putObject("restowUnits");
      restow.put("totalUnits", "bad");
      restow.put("size20Units", "bad");
      restow.put("size40Units", "bad");
      restow.put("size45Units", "bad");

      assertTrue(isFail(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenTotalUnitsIsNumeric() {
      addMovesForecasts(event).putObject("restowUnits").put("totalUnits", 5);
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenSize20UnitsIsNumeric() {
      addMovesForecasts(event).putObject("restowUnits").put("size20Units", 5);
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenSize40UnitsIsNumeric() {
      addMovesForecasts(event).putObject("restowUnits").put("size40Units", 5);
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenSize45UnitsIsNumeric() {
      addMovesForecasts(event).putObject("restowUnits").put("size45Units", 5);
      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_fails_whenOneOfTwoOccurrencesIsInvalid() {
      addMovesForecasts(event).putObject("restowUnits").put("size20Units", 8);
      addMovesForecasts(event).putObject("restowUnits").put("size20Units", "bad");

      assertTrue(isFail(PortCallChecks.restowUnitsSizeCheck(), body));
    }

    @Test
    void restowUnitsSize_passes_whenBothOccurrencesAreValid() {
      addMovesForecasts(event).putObject("restowUnits").put("size20Units", 8);
      addMovesForecasts(event).putObject("restowUnits").put("totalUnits", 3);

      assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }
  }

  @Nested
  class LoadAndDischargeUnitsSizeChecksTest {
    @Test
    void loadUnitsTotalUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("totalUnits")
        .put("totalUnits", 20);
      assertTrue(isOk(PortCallChecks.loadUnitsTotalUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsTotalUnitsSize_fails_whenNotNumeric() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("totalUnits")
        .put("totalUnits", "x");
      assertTrue(isFail(PortCallChecks.loadUnitsTotalUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsLadenUnitsSize_fails_whenNotNumeric() {
      ObjectNode mf = addMovesForecasts(event);
      mf.putObject("loadUnits").putObject("ladenUnits").put("totalUnits", "x");
      assertTrue(isFail(PortCallChecks.loadUnitsLadenUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsLadenUnitsSize_passes_whenAbsent() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("totalUnits")
        .put("totalUnits", 3);
      assertTrue(isOk(PortCallChecks.loadUnitsLadenUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsEmptyUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("emptyUnits")
        .put("size20Units", 3);
      assertTrue(isOk(PortCallChecks.loadUnitsEmptyUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsPluggedReeferUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("pluggedReeferUnits")
        .put("size40Units", 3);
      assertTrue(isOk(PortCallChecks.loadUnitsPluggedReeferUnitsSizeCheck(), body));
    }

    @Test
    void loadUnitsOutOfGaugeUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("outOfGaugeUnits")
        .put("size45Units", 3);
      assertTrue(isOk(PortCallChecks.loadUnitsOutOfGaugeUnitsSizeCheck(), body));
    }

    @Test
    void dischargeUnitsTotalUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("dischargeUnits")
        .putObject("totalUnits")
        .put("totalUnits", 20);
      assertTrue(isOk(PortCallChecks.dischargeUnitsTotalUnitsSizeCheck(), body));
    }

    @Test
    void dischargeUnitsLadenUnitsSize_fails_whenNotNumeric() {
      addMovesForecasts(event)
        .putObject("dischargeUnits")
        .putObject("ladenUnits")
        .put("totalUnits", "x");
      assertTrue(isFail(PortCallChecks.dischargeUnitsLadenUnitsSizeCheck(), body));
    }

    @Test
    void dischargeUnitsEmptyUnitsSize_passes_whenAbsent() {
      addMovesForecasts(event)
        .putObject("loadUnits")
        .putObject("emptyUnits")
        .put("totalUnits", 1);
      assertTrue(isOk(PortCallChecks.dischargeUnitsEmptyUnitsSizeCheck(), body));
    }

    @Test
    void dischargeUnitsPluggedReeferUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("dischargeUnits")
        .putObject("pluggedReeferUnits")
        .put("size20Units", 3);
      assertTrue(isOk(PortCallChecks.dischargeUnitsPluggedReeferUnitsSizeCheck(), body));
    }

    @Test
    void dischargeUnitsOutOfGaugeUnitsSize_passes_whenNumeric() {
      addMovesForecasts(event)
        .putObject("dischargeUnits")
        .putObject("outOfGaugeUnits")
        .put("size40Units", 3);
      assertTrue(isOk(PortCallChecks.dischargeUnitsOutOfGaugeUnitsSizeCheck(), body));
    }
  }

  @Nested
  class ScenarioChecksAggregationTest {
    @Test
    void timestampScenarioChecks_returnsThreeChecks() {
      assertEquals(3, PortCallChecks.timestampScenarioChecks().size());
    }

    @Test
    void movesForecastsScenarioChecks_returnsFifteenChecks() {
      assertEquals(15, PortCallChecks.movesForecastsScenarioChecks().size());
    }


    @Test
    void multipleEvents_onlyOneOfTwoValidCategoryOccurrences_stillFailsNow() {
      addMovesForecasts(event).putObject("loadUnits");

      ObjectNode event2 = addEvent();
      ObjectNode mf2 = addMovesForecasts(event2);
      mf2.putObject("loadUnits").put("ladenUnits", 5);

      assertFalse(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
    }

    @Test
    void multipleEvents_onlyOneOfTwoValidSizeOccurrences_stillFailsNow() {
      addMovesForecasts(event).putObject("restowUnits").put("size20Units", "x");

      ObjectNode event2 = addEvent();
      addMovesForecasts(event2).putObject("restowUnits").put("size20Units", 8);

      assertFalse(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
    }
  }

  @Nested
  class PayloadChecksEntryPointsTest {
    @Test
    void getPortCallPostPayloadChecks_returnsNonNullCheck_forTimestampScenario() {
      assertNotNull(
        PortCallChecks.getPortCallPostPayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters("TIMESTAMP", null, null)));
    }

    @Test
    void getPortCallPostPayloadChecks_returnsNonNullCheck_forMoveForecastScenario() {
      assertNotNull(
        PortCallChecks.getPortCallPostPayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters("MOVE_FORECAST", null, null)));
    }

    @Test
    void getPortCallPostPayloadChecks_returnsNonNullCheck_forUnrecognizedScenarioType() {
      assertNotNull(
        PortCallChecks.getPortCallPostPayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters(null, null, null)));
    }

    @Test
    void getGetResponsePayloadChecks_returnsNonNullCheck_forTimestampScenario() {
      assertNotNull(
        PortCallChecks.getGetResponsePayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters("TIMESTAMP", null, null)));
    }

    @Test
    void getGetResponsePayloadChecks_returnsNonNullCheck_forMoveForecastScenario() {
      assertNotNull(
        PortCallChecks.getGetResponsePayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters("MOVE_FORECAST", null, null)));
    }

    @Test
    void getGetResponsePayloadChecks_returnsNonNullCheck_forUnrecognizedScenarioType() {
      assertNotNull(
        PortCallChecks.getGetResponsePayloadChecks(
          UUID.randomUUID(), "2.0.0", () -> new DynamicScenarioParameters(null, null, null)));
    }
  }
}
