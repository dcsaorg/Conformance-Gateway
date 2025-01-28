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
      jsonNode.required("carrierBookingReference").asText(),
      jsonNode.path("commoditySubReference").asText(null),
      jsonNode.path("commoditySubReference2").asText(null),
      jsonNode.required("equipmentReference").asText(),
      jsonNode.path("equipmentReference2").asText(null),
      jsonNode.required("invoicePayableAtUNLocationCode").asText(),
      jsonNode.required("consignmentItemHSCode").asText(),
      jsonNode.path("consignmentItem2HSCode").asText(null),
      jsonNode.required("descriptionOfGoods").asText(),
      jsonNode.path("descriptionOfGoods2").asText(null),
      jsonNode.path("outerPackagingDescription").asText(null)
    );
  }
}
