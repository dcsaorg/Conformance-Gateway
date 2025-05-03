package org.dcsa.conformance.specifications.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.an.v100.OpenApiToolkit;
import org.dcsa.conformance.specifications.an.v100.SchemaOverride;
import org.dcsa.conformance.specifications.an.v100.constraints.AttributeOneRequiresAttributeTwo;
import org.dcsa.conformance.specifications.an.v100.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.an.v100.types.CarrierClause;
import org.dcsa.conformance.specifications.an.v100.types.CarrierCodeListProvider;
import org.dcsa.conformance.specifications.an.v100.types.ContainerLoadTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.DestinationDeliveryTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.an.v100.types.TransportDocumentReference;
import org.dcsa.conformance.specifications.an.v100.types.TransportDocumentTypeCode;

@Data
@Schema(description = "Full content of an Arrival Notice document.")
public class ArrivalNotice {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "The date when the Arrival Notice was issued.")
  private FormattedDate issueDate;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      type = "string",
      pattern = "^\\S+$",
      maxLength = 4,
      example = "HLCU",
      description =
          "Code in the list provided by `carrierCodeListProvider` of the carrier publishing the Arrival Notice")
  private String carrierCode;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "The provider of the code list in which `carrierCode` is defined.")
  private CarrierCodeListProvider carrierCodeListProvider;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "ATTRIBUTE The party to contact for any inquiries related to this Arrival Notice.")
  @ArraySchema(minItems = 1)
  private List<ContactInformation> carrierContactInformation;

  @Schema(
      description =
"""
The party to contact in relation to the cargo release (e.g. a shipping agency other than the POD carrier agency).
""")
  private List<ContactInformation> carrierInformationForCargoRelease;

  @Schema(allOf = Location.class)
  @SchemaOverride(description = "Pickup location")
  private Object pickupLocation;

  @Schema(allOf = Location.class)
  @SchemaOverride(description = "Return location")
  private Object returnLocation;

  @Schema(
      maxLength = 500,
      description = "Return instructions",
      example = "Please place the container...")
  private String returnInstructions;

  @Schema(
      maxLength = 1000,
      description = "Customs import declaration procedure",
      example = "The tax must be declared...")
  private String customsImportDeclarationProcedure;

  @Schema(
      maxLength = 500,
      example = "FIRMS code: B986",
      description =
"""
Free text field to provide additional required information for the consignee to prepare for the shipment arrival,
e.g. additional required documents to prepare and present for shipment release - country specific.
""")
  private String additionalInformation;

  @Schema(
      description =
"""
References provided by the shipper or freight forwarder at the time of Booking or at the time of providing
Shipping Instructions. Carriers share them back when providing Track & Trace event updates, some are also printed
on the B/L. Customers can use these references to track shipments in their internal systems.
""")
  private List<Reference> references;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "Reference of the transport document for which this arrival notice was created")
  private TransportDocumentReference transportDocumentReference;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description = "Type of the transport document for which this arrival notice was created")
  private TransportDocumentTypeCode transportDocumentTypeCode;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      name = "isElectronic",
      type = "boolean",
      example = "true",
      description =
"""
Flag indicating whether the transport document for which this arrival notice was created is electronically transferred
""")
  private boolean electronic;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "Code representing the delivery type offered at destination")
  private DestinationDeliveryTypeCode deliveryTypeAtDestination;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(
      description =
"""
Code indicating whether at destination the unloaded cargo occupies an entire container (FCL)
or shares the container with other shipments (LCL).
""",
      example = "FCL")
  private ContainerLoadTypeCode cargoMovementTypeAtDestination;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50000,
      description = "Carrier terms and conditions of transport.",
      example = "These terms and conditions define...")
  private String termsAndConditions;

  @Schema(description = "List of carrier clauses")
  private List<CarrierClause> carrierClauses;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Document parties")
  @ArraySchema(minItems = 1)
  private List<DocumentParty> documentParties;

  @Schema() private VesselVoyage vesselVoyage;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Transport transport;

  @Schema(description = "List of free time conditions applicable to this shipment at destination")
  private List<FreeTime> freeTime;

  @Schema(description = "List of charges applicable to this shipment")
  private List<Charge> charges;

  @Schema(allOf = Location.class)
  @SchemaOverride(
      description =
"""
Location where the customer will make the payment of ocean freight and charges for the main transport,
typically expressed as a UN/LOCODE or just a location name.
""")
  private Object invoicePayableAt;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The equipments being used.")
  @ArraySchema(minItems = 1)
  private List<UtilizedTransportEquipment> utilizedTransportEquipments;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The list of consignment items in the shipment.")
  @ArraySchema(minItems = 1)
  private List<ConsignmentItem> consignmentItems;

  @SchemaOverride(description = "Visualization of an arrival notice, as an embedded document")
  private EmbeddedDocument arrivalNoticeVisualization;

  public static List<SchemaConstraint> getConstraints() {
    return List.of(
      new AttributeOneRequiresAttributeTwo(
        OpenApiToolkit.getClassField(ArrivalNotice.class, "carrierCodeListProvider"),
        OpenApiToolkit.getClassField(ArrivalNotice.class, "carrierCode")));
  }
}
