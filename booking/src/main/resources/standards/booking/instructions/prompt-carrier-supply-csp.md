The DCSA reference implementation of a shipper running in the sandbox needs to exchange API calls with your carrier application in order to measure its conformance, but out of the box the DCSA shipper does not have any information about your organization's data. Therefore, at the beginning of the scenario you need to provide a number of parameters, which the DCSA shipper will use to customize the requests and responses that it sends to your carrier application throughout the scenario.

The DCSA shipper uses the following parameters:
* `serviceContractReference` (mandatory): copied into the `/serviceContractReference` attribute of the booking submitted in UC1
* `contractQuotationReference` (optional): copied into the `/contractQuotationReference` attribute of the booking submitted in UC1
* `carrierExportVoyageNumber` (optional): copied into the `/carrierExportVoyageNumber` attribute of the booking submitted in UC1
* `carrierServiceName` (optional): copied into the `/carrierServiceName` attribute of the booking submitted in UC1
* `hsCodes1` (optional): copied into each element of the `/requestedEquipments` array into the first element of `commodities` as the only element of `HSCodes`
* `commodityType1` (optional): copied into each element of the `/requestedEquipments` array into the first element of `commodities` as the value of attribute `commodityType`
* `hsCodes2` (optional): copied into each element of the `/requestedEquipments` array into the second element of `commodities` as the only element of `HSCodes`
* `commodityType2` (optional): copied into each element of the `/requestedEquipments` array into the second element of `commodities` as the value of attribute `commodityType`
* `polUNLocationCode` (optional): copied into the element of the `/shipmentLocations` array with `locationTypeCode: POL` as the value of attribute `location/UNLocationCode`
* `podUNLocationCode` (optional): copied into the element of the `/shipmentLocations` array with `locationTypeCode: POD` as the value of attribute `location/UNLocationCode`

Provide the scenario parameters in this JSON format, adjusting the value of each parameter as needed so that your carrier application can complete the scenario normally and deleting any optional attributes that are not needed:
