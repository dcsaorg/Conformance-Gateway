package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(
    String serviceContractReference,
    String contractQuotationReference,
    String carrierExportVoyageNumber,
    String carrierServiceName,
    String hsCodes1,
    String commodityType1,
    String hsCodes2,
    String commodityType2,
    String polUNLocationCode,
    String podUNLocationCode) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, CarrierScenarioParameters.class);
  }
}
