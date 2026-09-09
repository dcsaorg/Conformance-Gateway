package org.dcsa.conformance.standards.an.action;

import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.standards.an.ANComponentFactory;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublisherPostANActionTest {

  private static final ANComponentFactory FACTORY =
    new ANComponentFactory("AN", "1.0.0", "Conformance");
  private static final String CUSTOM_CHECK_TITLE =
    "The AN Producer has correctly demonstrated the use of functionally required attributes in the payload";

  @Test
  void producerPostScenarioIncludesProducerCustomPayloadValidations() {
    PublisherPostANAction action =
      new PublisherPostANAction(
        "Producer1",
        "Consumer1",
        null,
        ScenarioType.BASIC,
        FACTORY.getMessageSchemaValidator("PostArrivalNoticesRequest"),
        "POST Arrival Notice (BASIC)");

    assertTrue(subCheckTitles(action).contains(CUSTOM_CHECK_TITLE));
  }

  @Test
  void consumerPostScenarioContainsDefaultValidationsOnly() {
    PublisherPostANAction action =
      new PublisherPostANAction(
        "Producer1",
        "Consumer1",
        null,
        ScenarioType.BASIC,
        FACTORY.getMessageSchemaValidator("PostArrivalNoticesRequest"),
        "POST Arrival Notice",
        false);

    assertFalse(subCheckTitles(action).contains(CUSTOM_CHECK_TITLE));
  }

  private static List<String> subCheckTitles(PublisherPostANAction action) {
    ConformanceCheck check = action.createCheck("1.0.0");
    return check.subChecksStream().map(ConformanceCheck::getTitle).map(String::stripLeading).toList();
  }
}

