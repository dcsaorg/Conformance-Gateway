package org.dcsa.conformance.standards.an.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "**Arrival Notice** published.")
public class ArrivalNotice {

  @Schema(description = "Date when the Arrival Notice was issued.", example = "2025-01-23")
  private String issueDate;

  @Schema(
      description = "The NMFTA or the SMDG code of the issuing carrier of the Arrival Notice.",
      example = "HLCU")
  private String carrierCode;

  @Schema(
      description =
          """
The provider used for identifying the issuer Code. Possible values are:

 * SMDG (Ship Message Design Group)
 * NMFTA (National Motor Freight Traffic Association) includes SPLC (Standard Point Location Code)
""",
      example = "NMFTA")
  private String carrierCodeListProvider;

  @Schema(
      description = "The party to contact in case of questions in relation to the Arrival Notice.")
  private List<Contact> carrierContactInformation;

  @Schema(
      description =
          "The party to contact in relation to the cargo release (e.g. a shipping agency other than the POD carrier agency).")
  private List<Contact> carrierInformationForCargoRelease;

  @Schema(description = "Pickup location")
  private Location pickupLocation;

  @Schema(description = "Return location")
  private Location returnLocation;

  @Schema(description = "Return instructions", example = "Please place the container...")
  private String returnInstructions;

  @Schema(
      description = "Customs import declaration procedure",
      example = "The tax must be declared...")
  private String customsImportDeclarationProcedure;

  @Schema(description = "Additional information", example = "FIRMS code: B986")
  private String additionalInformation;

  @Schema(
      description =
          """
References provided by the shipper or freight forwarder at the time of Booking or at the time of providing Shipping Instructions.
Carriers share it back when providing Track & Trace event updates, some are also printed on the B/L.
Customers can use these references to track shipments in their internal systems.
""")
  private List<Reference> references;

  @Schema(
      description = "Reference of the transport document for which this arrival notice was created",
      example = "XYZ1234")
  private String transportDocumentReference;

  @Schema(
      description =
          """
The type of the transport document:
 * BOL (Bill of Lading)
 * SWB (Sea Waybill)
""",
      example = "BOL")
  private String transportDocumentTypeCode;

  @Schema(
      name = "isElectronic",
      description =
          "Flag indicating whether or not the transport document is electronically transferred",
      example = "true")
  private boolean electronic;

  @Schema(
      description =
          """
The type of service offered at destination. The options are:
 * CY (Container yard (incl. rail ramp))
 * SD (Store Door)
 * CFS (Container Freight Station)
""",
      example = "CY")
  private String deliveryTypeAtDestination;

  @Schema(
      description =
          """
The shipment term at the unloading of the cargo out of the container. Possible values are:
 * FCL (Full Container Load)
 * LCL (Less than Container Load)
""",
      example = "FCL")
  private String cargoMovementTypeAtDestination;

  @Schema(
      description = "Carrier terms and conditions of transport.",
      example = "These terms and conditions define...")
  private String termsAndConditions;

  @Schema(
      description =
          """
Clauses for a specific shipment added by the carrier, subject to local rules / guidelines
 or certain mandatory information required to be shared with the customer.""")
  private List<String> carrierClauses;

  @Schema(description = "Document parties")
  private DocumentParties documentParties;

  @Schema(description = "Vessel voyage")
  private VesselVoyage vesselVoyage;

  @Schema(description = "Transport info")
  private VesselVoyage transport;

  @Schema(description = "List of free time conditions applicable to this shipment at destination.")
  private List<FreeTime> freeTime;

  @Schema(description = "List of charges applicable to this shipment.")
  private List<Charge> charges;

  @Schema(
      description =
          "Location where payment of ocean freight and charges for the main transport will take place by the customer.")
  private InvoicePayableAt invoicePayableAt;

  @Schema(description = "The equipments being used.")
  private List<UtilizedTransportEquipment> utilizedTransportEquipments;

  @Schema(description = "The list of consignment items in the shipment.")
  private List<ConsignmentItem> consignmentItems;
}
