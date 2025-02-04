package org.dcsa.conformance.standards.ebl.party;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.party.ScenarioParameters;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarrierScenarioParameters(
    String carrierBookingReference,
    String commoditySubReference,
    String commoditySubReference2,
    String equipmentReference,
    String equipmentReference2,
    String invoicePayableAtUNLocationCode,
    String consignmentItemHSCode,
    String consignmentItem2HSCode,
    String descriptionOfGoods,
    String descriptionOfGoods2,
    String outerPackagingDescription)
    implements ScenarioParameters {

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
        jsonNode.path("carrierBookingReference").asText(null),
        jsonNode.path("commoditySubReference").asText(null),
        jsonNode.path("commoditySubReference2").asText(null),
        jsonNode.path("equipmentReference").asText(null),
        jsonNode.path("equipmentReference2").asText(null),
        jsonNode.path("invoicePayableAtUNLocationCode").asText(null),
        jsonNode.path("consignmentItemHSCode").asText(null),
        jsonNode.path("consignmentItem2HSCode").asText(null),
        jsonNode.path("descriptionOfGoods").asText(null),
        jsonNode.path("descriptionOfGoods2").asText(null),
        jsonNode.path("outerPackagingDescription").asText(null));
  }
}
