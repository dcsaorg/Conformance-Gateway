package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.standards.dt.v100.types.TransportDocumentReference;

@Data
@Schema(
    description =
"""
Simplified definition of a Transport Document meant to be used exclusively for AN - eBL mapping
""")
public class TransportDocument {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "Reference of the transport document")
  private TransportDocumentReference transportDocumentReference;

  @Schema() private String transportDocumentSubReference;

  @Schema() private String shippingInstructionsReference;

  @Schema() private String transportDocumentStatus;

  @Schema() private String transportDocumentTypeCode;

  @Schema() private String isShippedOnBoardType;

  @Schema() private String freightPaymentTermCode;

  @Schema() private String isElectronic;

  @Schema() private String isToOrder;

  @Schema() private String numberOfCopiesWithCharges;

  @Schema() private String numberOfCopiesWithoutCharges;

  @Schema() private String numberOfOriginalsWithCharges;

  @Schema() private String numberOfOriginalsWithoutCharges;

  @Schema() private String displayedNameForPlaceOfReceipt;

  @Schema() private String displayedNameForPortOfLoad;

  @Schema() private String displayedNameForPortOfDischarge;

  @Schema() private String displayedNameForPlaceOfDelivery;

  @Schema() private String shippedOnBoardDate;

  @Schema() private String displayedShippedOnBoardReceivedForShipment;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50000,
      description = "Carrier terms and conditions of transport.",
      example = "Any reference in...")
  private String termsAndConditions;

  @Schema() private String receiptTypeAtOrigin;

  @Schema() private String deliveryTypeAtDestination;

  @Schema() private String cargoMovementTypeAtOrigin;

  @Schema() private String cargoMovementTypeAtDestination;

  @Schema() private String issueDate;

  @Schema() private String receivedForShipmentDate;

  @Schema() private String serviceContractReference;

  @Schema() private String contractQuotationReference;

  @Schema() private String declaredValue;

  @Schema() private String declaredValueCurrency;

  @Schema() private String carrierCode;

  @Schema() private String carrierCodeListProvider;

  @Schema() private String carrierClauses;

  @Schema() private String numberOfRiderPages;

  @Schema() private String transports;

  @Schema() private String charges;

  @Schema() private String placeOfIssue;

  @Schema() private String invoicePayableAt;

  @Schema() private String partyContactDetails;

  @Schema() private String documentParties;

  @Schema() private String consignmentItems;

  @Schema() private String utilizedTransportEquipments;

  @Schema() private String exportLicense;

  @Schema() private String importLicense;

  @Schema() private String references;

  @Schema() private String customsReferences;
}
