package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

import java.util.List;

@Data
@Schema(
    description =
"""
Simplified definition of a Transport Document meant to be used exclusively for AN - eBL mapping
""")
public class TransportDocument {

  private UnspecifiedType transportDocumentReference;

  private UnspecifiedType transportDocumentSubReference;

  private UnspecifiedType shippingInstructionsReference;

  private UnspecifiedType transportDocumentStatus;

  private UnspecifiedType transportDocumentTypeCode;

  private UnspecifiedType isShippedOnBoardType;

  private UnspecifiedType freightPaymentTermCode;

  private UnspecifiedType isElectronic;

  private UnspecifiedType isToOrder;

  private UnspecifiedType numberOfCopiesWithCharges;

  private UnspecifiedType numberOfCopiesWithoutCharges;

  private UnspecifiedType numberOfOriginalsWithCharges;

  private UnspecifiedType numberOfOriginalsWithoutCharges;

  private UnspecifiedType displayedNameForPlaceOfReceipt;

  private UnspecifiedType displayedNameForPortOfLoad;

  private UnspecifiedType displayedNameForPortOfDischarge;

  private UnspecifiedType displayedNameForPlaceOfDelivery;

  private UnspecifiedType shippedOnBoardDate;

  private UnspecifiedType displayedShippedOnBoardReceivedForShipment;

  private UnspecifiedType termsAndConditions;

  private UnspecifiedType receiptTypeAtOrigin;

  private UnspecifiedType deliveryTypeAtDestination;

  private UnspecifiedType cargoMovementTypeAtOrigin;

  private UnspecifiedType cargoMovementTypeAtDestination;

  private UnspecifiedType issueDate;

  private UnspecifiedType receivedForShipmentDate;

  private UnspecifiedType serviceContractReference;

  private UnspecifiedType contractQuotationReference;

  private UnspecifiedType declaredValue;

  private UnspecifiedType declaredValueCurrency;

  private UnspecifiedType carrierCode;

  private UnspecifiedType carrierCodeListProvider;

  private UnspecifiedType carrierClauses;

  private UnspecifiedType numberOfRiderPages;

  private Transports transports; // NOT a list

  private List<Charge> charges;

  private PlaceOfIssue placeOfIssue;

  private InvoicePayableAt invoicePayableAt;

  private List<PartyContactDetail> partyContactDetails;

  private DocumentParties documentParties; // NOT a list

  private UnspecifiedType consignmentItems; // FIXME

  private UnspecifiedType utilizedTransportEquipments; // FIXME

  private UnspecifiedType exportLicense; // FIXME

  private UnspecifiedType importLicense; // FIXME

  private UnspecifiedType references; // FIXME

  private UnspecifiedType customsReferences; // FIXME
}
