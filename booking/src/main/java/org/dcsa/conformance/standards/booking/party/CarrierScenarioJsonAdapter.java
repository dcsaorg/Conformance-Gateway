package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

public class CarrierScenarioJsonAdapter {

  public static final String COMMODITY_TYPE_1 = "commodityType1";
  public static final String COMMODITY_TYPE_2 = "commodityType2";

  private CarrierScenarioJsonAdapter() {}

  public static ObjectNode toJson(CarrierScenarioParameters params, ScenarioType scenarioType) {
    ObjectNode json = params.toJson();

    if (ScenarioType.DG.equals(scenarioType)) {
      json.remove(COMMODITY_TYPE_1);
      json.remove(COMMODITY_TYPE_2);
    }

    return json;
  }
}
