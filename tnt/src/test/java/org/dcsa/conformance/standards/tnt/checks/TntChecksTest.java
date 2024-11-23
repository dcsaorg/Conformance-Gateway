package org.dcsa.conformance.standards.tnt.checks;

import static org.junit.jupiter.api.Assertions.*;
import static org.dcsa.conformance.standards.tnt.party.TntFilterParameter.*;

import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

class TntChecksTest {
  private static final OffsetDateTime TODAY = OffsetDateTime.now();
  private static final OffsetDateTime TOMORROW = TODAY.plusDays(1);

  @Test
  void testValidateEventCreatedDateTime() {
    _check(TODAY, EVENT_CREATED_DATE_TIME_LT, TODAY, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_LTE, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_EQ, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GTE, TODAY, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GT, TODAY, false);

    _check(TODAY, EVENT_CREATED_DATE_TIME_LT, TOMORROW, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME_LTE, TOMORROW, true);
    _check(TODAY, EVENT_CREATED_DATE_TIME, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_EQ, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GTE, TOMORROW, false);
    _check(TODAY, EVENT_CREATED_DATE_TIME_GT, TOMORROW, false);

    _check(TOMORROW, EVENT_CREATED_DATE_TIME_LT, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_LTE, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_EQ, TODAY, false);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_GTE, TODAY, true);
    _check(TOMORROW, EVENT_CREATED_DATE_TIME_GT, TODAY, true);
  }

  private void _check(
      OffsetDateTime left,
      TntFilterParameter operator,
      OffsetDateTime right,
      boolean expectedResult) {
    String validationError = TntChecks._validateEventCreatedDateTime(left, operator, right);
    if (expectedResult) {
      assertNull(
          validationError,
          "Expected %s %s %s to return no error but got %s"
              .formatted(left, operator.getQueryParamName(), right, validationError));
    } else {
      assertNotNull(
          validationError,
          "Expected %s %s %s to return an error but got none"
              .formatted(left, operator.getQueryParamName(), right));
    }
  }
}
