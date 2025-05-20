package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "The document that governs the terms of carriage between shipper and carrier for maritime transportation. Two distinct types of transport documents exist:\n- Bill of Lading\n- Sea Waybill.")
@Data
public class TransportDocument {

  @Schema(description = "A unique number allocated by the shipping line to the transport document and the main number used for the tracking of the status of the shipment.", example = "HHL71800000", maxLength = 20, pattern = "^\\S(?:.*\\S)?$")
  private String transportDocumentReference;

  @Schema(description = "Additional reference that can be optionally used alongside the `transportDocumentReference` in order to distinguish between versions of the same `Transport Document`.", example = "Version_1", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String transportDocumentSubReference;

  @Schema(description = "The identifier for a `Shipping Instructions` provided by the carrier for system purposes.", example = "e0559d83-00e2-438e-afd9-fdd610c1a008", maxLength = 100, pattern = "^\\S(?:.*\\S)?$")
  private String shippingInstructionsReference;

  @Schema(description = "The status of the `Transport Document`. Possible values are:\n- DRAFT\n- APPROVED\n- ISSUED\n- PENDING_SURRENDER_FOR_AMENDMENT\n- SURRENDERED_FOR_AMENDMENT\n- PENDING_SURRENDER_FOR_DELIVERY\n- SURRENDERED_FOR_DELIVERY\n- VOIDED", example = "DRAFT", maxLength = 50)
  private String transportDocumentStatus;

  @Schema(description = "Specifies the type of the transport document\n- `BOL` (Bill of Lading)\n- `SWB` (Sea Waybill)", example = "SWB", allowableValues = {"BOL", "SWB"})
  private String transportDocumentTypeCode;

  @Schema(description = "Specifies whether the Transport Document is a received for shipment, or shipped on board.", example = "true")
  private Boolean isShippedOnBoardType;

  @Schema(description = "An indicator of whether freight and ancillary fees for the main transport are prepaid (`PRE`) or collect (`COL`).\n\n- `PRE` (Prepaid)\n- `COL` (Collect)", example = "PRE", allowableValues = {"PRE", "COL"})
  private String freightPaymentTermCode;

  @Schema(description = "An indicator whether the transport document is electronically transferred.", example = "true")
  private Boolean isElectronic;

  @Schema(description = "Indicates whether the B/L is issued `to order` or not.", example = "false")
  private Boolean isToOrder;

  @Schema(description = "The requested number of copies of the `Transport Document` to be issued by the carrier including charges.", example = "2", minimum = "0", format = "int32")
  private Integer numberOfCopiesWithCharges;

  @Schema(description = "The requested number of copies of the `Transport Document` to be issued by the carrier NOT including charges.", example = "2", minimum = "0", format = "int32")
  private Integer numberOfCopiesWithoutCharges;

  @Schema(description = "Number of originals of the Bill of Lading with charges.", example = "1", minimum = "0", format = "int32")
  private Integer numberOfOriginalsWithCharges;

  @Schema(description = "Number of originals of the Bill of Lading without charges.", example = "1", minimum = "0", format = "int32")
  private Integer numberOfOriginalsWithoutCharges;

  @Schema(description = "Display name for Place of Receipt.")
  private List<String> displayedNameForPlaceOfReceipt;

  @Schema(description = "Display name for Port of Load.")
  private List<String> displayedNameForPortOfLoad;

  @Schema(description = "Display name for Port of Discharge.")
  private List<String> displayedNameForPortOfDischarge;

  @Schema(description = "Display name for Place of Delivery.")
  private List<String> displayedNameForPlaceOfDelivery;

  @Schema(description = "Date goods were shipped on board.", example = "2020-12-12", format = "date")
  private String shippedOnBoardDate;

  @Schema(description = "Text to be displayed as shipped/received evidence.", example = "Received for Shipment CMA CGM CONCORDE 28-Jul-2022...", maxLength = 250, pattern = "^\\S(?:.*\\S)?$")
  private String displayedShippedOnBoardReceivedForShipment;

  @Schema(description = "Carrier terms and conditions of transport.", example = "Any reference in...", maxLength = 50000)
  private String termsAndConditions;

  @Schema(description = "Type of service offered at Origin.", example = "CY", maxLength = 3, allowableValues = {"CY", "SD", "CFS"})
  private String receiptTypeAtOrigin;

  @Schema(description = "Type of service offered at Destination.", example = "CY", maxLength = 3, allowableValues = {"CY", "SD", "CFS"})
  private String deliveryTypeAtDestination;

  @Schema(description = "Shipment term at loading.", example = "FCL", maxLength = 3)
  private String cargoMovementTypeAtOrigin;

  @Schema(description = "Shipment term at unloading.", example = "FCL", maxLength = 3)
  private String cargoMovementTypeAtDestination;

  @Schema(description = "Issue date of the document.", example = "2020-12-12", format = "date")
  private String issueDate;

  @Schema(description = "Date goods were received for shipment.", example = "2020-12-12", format = "date")
  private String receivedForShipmentDate;

  @Schema(description = "Reference number for the service contract.", example = "HHL51800000", maxLength = 30)
  private String serviceContractReference;

  @Schema(description = "Contract or quotation reference.", example = "HHL1401", maxLength = 35, pattern = "^\\S(?:.*\\S)?$")
  private String contractQuotationReference;

  @Schema(description = "Declared value of cargo.", example = "1231.1", minimum = "0", format = "float")
  private Double declaredValue;

  @Schema(description = "Currency for declared value.", example = "DKK", maxLength = 3, minLength = 3, pattern = "^[A-Z]{3}$")
  private String declaredValueCurrency;

  @Schema(description = "SCAC or SMDG code of the carrier.", example = "MMCU", maxLength = 4, pattern = "^\\S+$")
  private String carrierCode;

  @Schema(description = "Code list provider for the carrier.", example = "NMFTA", allowableValues = {"SMDG", "NMFTA"})
  private String carrierCodeListProvider;

  @Schema(description = "Additional clauses by the carrier.")
  private List<String> carrierClauses;

  @Schema(description = "Number of additional pages required.", example = "2", minimum = "0", format = "int32")
  private Integer numberOfRiderPages;

  @Schema
  private Transports transports;

  @Schema(description = "A list of Charges.")
  private List<String> charges;

  @Schema
  private PlaceOfIssue placeOfIssue;

  @Schema
  private InvoicePayableAt invoicePayableAt;

  @Schema(description = "Contact details related to the document.")
  private List<String> partyContactDetails;

  @Schema(description = "All `Parties` with associated roles.")
  private DocumentParties documentParties;

  @Schema(description = "A list of ConsignmentItems.")
  private List<String> consignmentItems;

  @Schema(description = "A list of utilized transport equipments.")
  private List<String> utilizedTransportEquipments;

  @Schema
  private ExportLicense exportLicense;

  @Schema
  private ImportLicense importLicense;

  @Schema(description = "A list of references.")
  private List<String> references;

  @Schema(description = "A list of customs references.")
  private List<String> customsReferences;
}
