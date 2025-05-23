package org.dcsa.conformance.specifications.standards.an.v100.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.generator.SchemaOverride;
import org.dcsa.conformance.specifications.constraints.AttributeOneRequiresAttributeTwo;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.standards.an.v100.types.CarrierClause;
import org.dcsa.conformance.specifications.standards.an.v100.types.CarrierCodeListProvider;
import org.dcsa.conformance.specifications.standards.an.v100.types.ContainerLoadTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.DestinationDeliveryTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportDocumentReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportDocumentTypeCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Charge;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ConsignmentItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Reference;
import org.dcsa.conformance.specifications.standards.dt.v100.model.UtilizedTransportEquipment;

@Data
@Schema(description = "Full content of an Arrival Notice document.")
public class ArrivalNotice {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @SchemaOverride(description = "The date and time when the Arrival Notice was issued.")
  private FormattedDateTime issueDateTime;

  @Schema(
      type = "string",
      maxLength = 1000,
      example = "Warning",
      description =
"""
Free text used to indicate a certain version or type of arrival notice,
for example "Warning", "Updated", "Second", "Third" etc.
""")
  private String label;

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

  @SchemaOverride(description = "Pickup location")
  private Location pickupLocation;

  @Schema()
  @SchemaOverride(description = "Return location")
  private Location returnLocation;

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
      maxLength = 100,
      description =
"""
Reference number for agreement between shipper and carrier, which optionally includes a certain minimum
quantity commitment (usually referred as "MQC") of cargo that the shipper commits to over a fixed period,
and the carrier commits to a certain rate or rate schedule.
""",
      example = "SCN12345")
  private String serviceContractNumber;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      maxLength = 50000,
      description = "Carrier terms and conditions of transport.",
      example = "Any reference in...")
  private String termsAndConditions;

  @Schema(description = "List of carrier clauses")
  private List<CarrierClause> carrierClauses;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Document parties")
  @ArraySchema(minItems = 1)
  private List<DocumentParty> documentParties;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Transport transport;

  @Schema(description = "List of free time conditions applicable to this shipment at destination")
  private List<FreeTime> freeTime;

  @Schema(description = "List of charges applicable to this shipment")
  private List<Charge> charges;

  @Schema(
      maxLength = 100,
      description =
"""
Name identifying the entity responsible for freight payment.
""",
      example = "Acme Inc.")
  private String payerCode;

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
            SpecificationToolkit.getClassField(ArrivalNotice.class, "carrierCodeListProvider"),
            SpecificationToolkit.getClassField(ArrivalNotice.class, "carrierCode")));
  }
}
