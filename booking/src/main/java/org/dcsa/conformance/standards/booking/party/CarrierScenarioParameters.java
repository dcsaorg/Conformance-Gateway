package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(String contractQuotationReference,
                                        String carrierExportVoyageNumber,
                                        String carrierServiceName,
                                        String  hsCodes,
                                        String commodityType,
                                        String polUNLocationCode,
                                        String podUNLocationCode
                                        ) {
  public ObjectNode toJson() {
    return new ObjectMapper()
        .createObjectNode()
        .put("contractQuotationReference", contractQuotationReference())
        .put("carrierExportVoyageNumber", carrierExportVoyageNumber())
        .put("carrierServiceName", carrierServiceName())
        .put("hsCodes", hsCodes() )
        .put("commodityType", commodityType() )
        .put("polUNLocationCode", polUNLocationCode() )
        .put("podUNLocationCode", podUNLocationCode() );
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode cspNode = (ObjectNode) jsonNode;

    return new CarrierScenarioParameters(
        cspNode.get("contractQuotationReference").asText(),
        cspNode.get("carrierExportVoyageNumber").asText(),
        cspNode.get("carrierServiceName").asText(),
        cspNode.get("hsCodes").asText(),
        cspNode.get("commodityType").asText(),
        cspNode.get("polUNLocationCode").asText(),
        cspNode.get("podUNLocationCode").asText());
  }
}
