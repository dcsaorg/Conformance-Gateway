The **DCSA reference implementation** of a shipper running in the sandbox needs to exchange API calls with your carrier application in order to measure its conformance. However, out of the box, the DCSA shipper does not have any information about your organization's data. Therefore, at the beginning of the scenario, you need to provide a number of parameters, which the DCSA shipper will use to customize the requests and responses that it sends to your carrier application throughout the scenario.

The DCSA shipper uses the following parameters:

- **`carrierBookingReference`** (mandatory): Copied into the `/carrierBookingReference` attribute of the shipping instructions submitted in UC1.
- **`commoditySubReference`** (optional): Copied into the `/commoditySubReference` attribute of the first element of the `consignmentItems` array in the shipping instructions submitted in UC1.
- **`commoditySubReference2`** (optional): Only applicable in scenarios **2c2u2e** and **2c2u1e**. Copied into the `/commoditySubReference` attribute of the second element of the `consignmentItems` array in the shipping instructions submitted in UC1.
- **`equipmentReference`** (mandatory): Copied into the `/equipmentReference` attribute of the first element in the `utilizedTransportEquipments` array and in the first element of the `cargoItems` array of the shipping instruction submitted in UC1.
- **`equipmentReference2`** (conditionally mandatory): Mandatory only for scenario **2c2u2e**. Copied into each element of the `/equipmentReference` attribute of the second element in the `utilizedTransportEquipments` array and in the first element of the `cargoItems` array of the shipping instruction submitted in UC1.
- **`invoicePayableAtUNLocationCode`** (mandatory): Copied into the `/UNLocationCode` attribute of `invoicePayableAt` object in the shipping instruction submitted in UC1.
- **`consignmentItemHSCode`** (mandatory): Copied into the `/HSCodes` attribute of the first element of the `consignmentItems` array in the shipping instruction submitted in UC1.
- **`consignmentItem2HSCode`** (conditionally mandatory): Only applicable and mandatory in scenarios **2c2u2e** and **2c2u1e**. Copied into the `/HSCodes` attribute of the second element of the `consignmentItems` array in the shipping instruction submitted in UC1.
- **`descriptionOfGoods`** (mandatory): Copied into the `/descriptionOfGoods` attribute of the first element of the `consignmentItems` array in the shipping instruction submitted in UC1.
- **`descriptionOfGoods2`** (conditionally mandatory): Only applicable and mandatory in scenarios **2c2u2e** and **2c2u1e**. Copied into the `/descriptionOfGoods` attribute of the second element of the `consignmentItems` array in the shipping instruction submitted in UC1.

### Scenario Definitions:
- **2c2u2e**: 2 consignment items, 2 utilized transport equipments, and 2 equipments.
- **2c2u1e**: 2 consignment items, 2 utilized transport equipments, and 1 equipment.
