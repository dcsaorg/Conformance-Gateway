package org.dcsa.conformance.standards.ebl.party;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CarrierScenarioParameters(
  String carrierBookingReference,
  String commoditySubreference,
  String commoditySubreference2,
  String equipmentReference,
  String equipmentReference2,
  String invoicePayableAtUNLocationCode,
  String consignmentItemHSCode,
  String consignmentItem2HSCode,
  String descriptionOfGoods,
  String descriptionOfGoods2,
  String outerPackagingDescription
  ) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
      jsonNode.required("carrierBookingReference").asText(),
      jsonNode.path("commoditySubreference").asText(null),
      jsonNode.path("commoditySubreference2").asText(null),
      jsonNode.path("equipmentReference").asText(null),
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
