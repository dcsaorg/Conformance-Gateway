package org.dcsa.conformance.standards.portcall;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.portcall.checks.PortCallChecks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PortCallChecksTest {
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

  // Helper methods
  private static boolean isOk(JsonContentCheck c, ObjectNode body) {
    return c.validate(body).getErrorMessages().isEmpty();
  }

  private static boolean isFail(JsonContentCheck c, ObjectNode body) {
    return !isOk(c, body);
  }

  private ObjectNode addMovesForecasts(ObjectNode event) {
    ArrayNode mfArr = event.putArray("movesForecasts");
    return mfArr.addObject();
  }

  // ============================================================
  // EVENT PRESENCE
  // ============================================================

  @Test
  void nonEmptyEvents_fails_when_missing_events() {
    ObjectNode body2 = mapper.createObjectNode(); // no events field
    assertTrue(isFail(PortCallChecks.nonEmptyEvents(), body2));
  }

  @Test
  void nonEmptyEvents_passes_when_one_event_present() {
    assertTrue(isOk(PortCallChecks.nonEmptyEvents(), body));
  }


  @Test
  void timestampClassifier_fails_when_missing_timestamp() {
    assertTrue(isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
  }

  @Test
  void timestampClassifier_fails_when_classifierCode_blank() {
    event.putObject("timestamp").put("classifierCode", "");
    assertTrue(isFail(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
  }

  @Test
  void timestampClassifier_passes_when_classifierCode_present() {
    event.putObject("timestamp").put("classifierCode", "EST");
    assertTrue(isOk(PortCallChecks.atLeastOneTimestampClassifierCodeCorrect(), body));
  }

  @Test
  void timestampServiceDateTime_fails_when_missing() {
    assertTrue(isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
  }

  @Test
  void timestampServiceDateTime_fails_when_blank() {
    event.putObject("timestamp").put("serviceDateTime", "");
    assertTrue(isFail(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
  }

  @Test
  void timestampServiceDateTime_passes_when_present() {
    event.putObject("timestamp").put("serviceDateTime", "2025-01-23T10:00:00Z");
    assertTrue(isOk(PortCallChecks.atLeastOneTimestampServiceDateTimeCorrect(), body));
  }


  @Test
  void movesForecastsPresence_fails_when_no_movesForecasts() {
    assertTrue(isFail(PortCallChecks.movesForecastsPresenceCheck(), body));
  }

  @Test
  void movesForecastsPresence_fails_when_movesForecasts_empty() {
    event.putArray("movesForecasts");
    assertTrue(isFail(PortCallChecks.movesForecastsPresenceCheck(), body));
  }

  @Test
  void movesForecastsPresence_fails_when_no_unit_objects() {
    addMovesForecasts(event).put("carrierCode", "MAEU");
    assertTrue(isFail(PortCallChecks.movesForecastsPresenceCheck(), body));
  }

  @Test
  void movesForecastsPresence_passes_when_restowUnits_present() {
    addMovesForecasts(event).putObject("restowUnits").put("totalUnits", 5);
    assertTrue(isOk(PortCallChecks.movesForecastsPresenceCheck(), body));
  }

  @Test
  void movesForecastsPresence_passes_when_loadUnits_present() {
    addMovesForecasts(event).putObject("loadUnits").putObject("totalUnits").put("totalUnits", 5);
    assertTrue(isOk(PortCallChecks.movesForecastsPresenceCheck(), body));
  }


  @Test
  void loadUnitsCategory_fails_when_empty() {
    addMovesForecasts(event).putObject("loadUnits");
    assertTrue(isFail(PortCallChecks.loadUnitsCategoryCheck(), body));
  }

  @Test
  void loadUnitsCategory_passes_when_totalUnits_present() {
    addMovesForecasts(event)
      .putObject("loadUnits")
      .putObject("totalUnits")
      .put("totalUnits", 10);
    assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
  }

  @Test
  void loadUnitsCategory_passes_when_ladenUnits_present() {
    addMovesForecasts(event)
      .putObject("loadUnits")
      .putObject("ladenUnits")
      .put("totalUnits", 4);
    assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
  }

  @Test
  void dischargeUnitsCategory_not_applicable_when_not_present() {
    addMovesForecasts(event)
      .putObject("loadUnits")
      .putObject("totalUnits")
      .put("totalUnits", 5);
    assertTrue(isOk(PortCallChecks.dischargeUnitsCategoryCheck(), body));
  }

  @Test
  void dischargeUnitsCategory_fails_when_present_but_empty() {
    addMovesForecasts(event).putObject("dischargeUnits");
    assertTrue(isFail(PortCallChecks.dischargeUnitsCategoryCheck(), body));
  }

  @Test
  void dischargeUnitsCategory_passes_when_ladenUnits_present() {
    addMovesForecasts(event)
      .putObject("dischargeUnits")
      .putObject("ladenUnits")
      .put("totalUnits", 10);
    assertTrue(isOk(PortCallChecks.dischargeUnitsCategoryCheck(), body));
  }

  @Test
  void restowUnitsSize_fails_when_all_invalid() {
    ObjectNode mf = addMovesForecasts(event);
    ObjectNode restow = mf.putObject("restowUnits");
    restow.put("totalUnits", "bad");
    restow.put("size20Units", "bad");

    assertTrue(isFail(PortCallChecks.restowUnitsSizeCheck(), body));
  }

  @Test
  void restowUnitsSize_passes_when_size20_present() {
    addMovesForecasts(event).putObject("restowUnits").put("size20Units", 5);
    assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
  }

  @Test
  void loadUnitsLadenUnitsSize_fails_when_invalid() {
    ObjectNode mf = addMovesForecasts(event);
    mf.putObject("loadUnits").putObject("ladenUnits").put("totalUnits", "x");
    assertTrue(isFail(PortCallChecks.loadUnitsLadenUnitsSizeCheck(), body));
  }

  @Test
  void loadUnitsLadenUnitsSize_passes_when_size40_present() {
    addMovesForecasts(event)
      .putObject("loadUnits")
      .putObject("ladenUnits")
      .put("size40Units", 3);
    assertTrue(isOk(PortCallChecks.loadUnitsLadenUnitsSizeCheck(), body));
  }

  @Test
  void dischargeUnitsTotalUnitsSize_passes_when_totalUnits_numeric() {
    addMovesForecasts(event)
      .putObject("dischargeUnits")
      .putObject("totalUnits")
      .put("totalUnits", 20);
    assertTrue(isOk(PortCallChecks.dischargeUnitsTotalUnitsSizeCheck(), body));
  }

  @Test
  void dischargeUnitsEmptyUnitsSize_not_applicable_when_not_present() {
    addMovesForecasts(event)
      .putObject("loadUnits")
      .putObject("ladenUnits")
      .put("size20Units", 1);
    assertTrue(isOk(PortCallChecks.dischargeUnitsEmptyUnitsSizeCheck(), body));
  }

  @Test
  void multipleEvents_onlyOneGoodCategoryExample_passes() {
    addMovesForecasts(event).putObject("loadUnits");

    ObjectNode event2 = ((ArrayNode) body.get("events")).addObject();
    ObjectNode mf2 = addMovesForecasts(event2);
    mf2.putObject("loadUnits").putObject("ladenUnits").put("totalUnits", 5);

    assertTrue(isOk(PortCallChecks.loadUnitsCategoryCheck(), body));
  }

  @Test
  void multipleEvents_onlyOneGoodSizeExample_passes() {

    addMovesForecasts(event).putObject("restowUnits").put("size20Units", "x");

    ObjectNode event2 = ((ArrayNode) body.get("events")).addObject();
    addMovesForecasts(event2).putObject("restowUnits").put("size20Units", 8);

    assertTrue(isOk(PortCallChecks.restowUnitsSizeCheck(), body));
  }
}
