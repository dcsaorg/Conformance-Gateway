package org.dcsa.conformance.standards.ebl.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ScenarioTypeTest {
  @Test
  void everyScenarioTypeHasConsistentMetadataAndAnExistingPayload() {
    for (ScenarioType type : ScenarioType.values()) {
      assertEquals(
          type == ScenarioType.REGULAR_STRAIGHT_BL || type == ScenarioType.REGULAR_NEGOTIABLE_BL
              ? "BOL"
              : "SWB",
          type.transportDocumentTypeCode());
      assertEquals(type == ScenarioType.REGULAR_NEGOTIABLE_BL, type.isToOrder());
      assertEquals(
          type == ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES, type.isReferencesRequired());
      assertEquals(type.isReferencesRequired(), type.isCustomsReferencesRequired());
      assertEquals(type == ScenarioType.DG, type.hasDG());
      assertEquals(type == ScenarioType.REGULAR_CLAD, type.isCarriersAgentAtDestinationRequired());
      assertEquals(
          Set.of(
                  ScenarioType.REGULAR_SWB,
                  ScenarioType.REGULAR_SWB_AMF,
                  ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES)
              .contains(type),
          type.isSWB());
      String name =
          switch (type) {
            case REGULAR_SWB -> "SWB";
            case REGULAR_STRAIGHT_BL -> "STRAIGHT_BL";
            case REGULAR_NEGOTIABLE_BL -> "NEGOTIABLE_BL";
            case REGULAR_2C_1U -> "2C_1U";
            case REGULAR_2C_2U -> "2C_2U";
            case REGULAR_SWB_SOC_AND_REFERENCES -> "SOC";
            case REGULAR_SWB_AMF -> "AMF";
            case REGULAR_NO_COMMODITY_SUBREFERENCE -> "";
            case REGULAR_CLAD -> "CLAD";
            case ACTIVE_REEFER, NON_OPERATING_REEFER, DG -> type.name();
          };
      assertEquals(name, type.getTDScenarioTypeName());
      String scope =
          switch (type) {
            case REGULAR_SWB -> "Sea Waybill";
            case REGULAR_STRAIGHT_BL -> "Straight B/L";
            case REGULAR_NEGOTIABLE_BL -> "Negotiable B/L";
            default -> name;
          };
      assertEquals(scope, type.tdScopeName());
      assertNotNull(getClass().getResource("/standards/ebl/messages/" + type.eblPayload("3.0.0")));
    }
  }
}
