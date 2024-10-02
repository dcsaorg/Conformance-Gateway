package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record CarrierScenarioParameters(
                                        String serviceContractReference,
                                        String contractQuotationReference,
                                        String carrierExportVoyageNumber,
                                        String carrierServiceName,
                                        String  hsCodes1,
                                        String commodityType1,
                                        String  hsCodes2,
                                        String commodityType2,
                                        String polUNLocationCode,
                                        String podUNLocationCode
                                        ) {
  public ObjectNode toJson() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("serviceContractReference", serviceContractReference())
        .put("contractQuotationReference", contractQuotationReference())
        .put("carrierExportVoyageNumber", carrierExportVoyageNumber())
        .put("carrierServiceName", carrierServiceName())
        .put("hsCodes1", hsCodes1() )
        .put("commodityType1", commodityType1() )
        .put("hsCodes2", hsCodes2() )
        .put("commodityType2", commodityType2() )
        .put("polUNLocationCode", polUNLocationCode() )
        .put("podUNLocationCode", podUNLocationCode() );
  }

  public static CarrierScenarioParameters fromJson(JsonNode jsonNode) {
    ObjectNode cspNode = (ObjectNode) jsonNode;

    return new CarrierScenarioParameters(
        cspNode.required("serviceContractReference").asText(),
        cspNode.required("contractQuotationReference").asText(),
        cspNode.required("carrierExportVoyageNumber").asText(),
        cspNode.required("carrierServiceName").asText(),
        cspNode.required("hsCodes1").asText(),
        cspNode.required("commodityType1").asText(),
        cspNode.required("hsCodes2").asText(),
        cspNode.required("commodityType2").asText(),
        cspNode.required("polUNLocationCode").asText(),
        cspNode.required("podUNLocationCode").asText());
  }
}
