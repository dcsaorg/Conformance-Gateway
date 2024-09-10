package org.dcsa.conformance.standards.ebl.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    var node = OBJECT_MAPPER
        .createObjectNode()
        .put("carrierBookingReference", carrierBookingReference)
        .put("equipmentReference", equipmentReference)
        .put("invoicePayableAtUNLocationCode", invoicePayableAtUNLocationCode)
        .put("consignmentItemHSCode", consignmentItemHSCode)
        .put("descriptionOfGoods", descriptionOfGoods);

    if (commoditySubreference != null) {
      node.put("commoditySubreference", commoditySubreference);
    }
    if (commoditySubreference2 != null) {
      node.put("commoditySubreference2", commoditySubreference2);
    }
    if (equipmentReference != null) {
      node.put("equipmentReference", equipmentReference);
    }
    if (equipmentReference2 != null) {
      node.put("equipmentReference2", equipmentReference2);
    }
    if (consignmentItem2HSCode != null) {
      node.put("consignmentItem2HSCode", consignmentItem2HSCode);
    }
    if (descriptionOfGoods2 != null) {
      node.put("descriptionOfGoods2", descriptionOfGoods2);
    }
    if (outerPackagingDescription != null) {
      node.put("outerPackagingDescription", outerPackagingDescription);
    }
    return node;
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
