package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(
  String carrierBookingReference,
  String commoditySubreference,
  String equipmentReference,
  String invoicePayableAtUNLocationCode,
  String consignmentItemHSCode,
  String descriptionOfGoods,
  String serviceContractReference,
  String contractQuotationReference
  ) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("carrierBookingReference", carrierBookingReference())
        .put("commoditySubreference", commoditySubreference())
        .put("equipmentReference", equipmentReference)
        .put("invoicePayableAtUNLocationCode", invoicePayableAtUNLocationCode)
        .put("consignmentItemHSCode", consignmentItemHSCode)
        .put("descriptionOfGoods", descriptionOfGoods)
        .put("serviceContractReference", serviceContractReference)
        .put("contractQuotationReference", contractQuotationReference);
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return new CarrierScenarioParameters(
      jsonNode.required("carrierBookingReference").asText(),
      jsonNode.required("commoditySubreference").asText(),
      jsonNode.required("equipmentReference").asText(),
      jsonNode.required("invoicePayableAtUNLocationCode").asText(),
      jsonNode.required("consignmentItemHSCode").asText(),
      jsonNode.required("descriptionOfGoods").asText(),
      jsonNode.required("serviceContractReference").asText(),
      jsonNode.required("contractQuotationReference").asText()
    );
  }
}
