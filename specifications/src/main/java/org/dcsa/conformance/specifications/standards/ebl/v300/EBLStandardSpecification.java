package org.dcsa.conformance.specifications.standards.ebl.v300;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.specifications.dataoverview.AttributesHierarchicalSheet;
import org.dcsa.conformance.specifications.dataoverview.DataOverviewSheet;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.generator.StandardSpecification;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ActiveReeferSettings;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Address;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoGrossVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoGrossWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoNetVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoNetWeight;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.CarriersAgentAtDestination;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Charge;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.City;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.Consignee;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ConsignmentItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CustomsReference;
import org.dcsa.conformance.specifications.standards.dt.v100.model.DangerousGoods;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.DocumentParties;
import org.dcsa.conformance.specifications.standards.dt.v100.model.EmergencyContactDetails;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.Endorsee;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Equipment;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ExportLicense;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Facility;
import org.dcsa.conformance.specifications.standards.dt.v100.model.GeoCoordinate;
import org.dcsa.conformance.specifications.standards.dt.v100.model.GrossWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.IdentifyingCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ImportLicense;
import org.dcsa.conformance.specifications.standards.dt.v100.model.InnerPackaging;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.InvoicePayableAt;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.IssuingParty;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Limits;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NationalCommodityCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetExplosiveContent;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetWeight;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.NotifyParty;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.OnwardInlandRouting;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.OtherDocumentParty;
import org.dcsa.conformance.specifications.standards.dt.v100.model.OuterPackaging;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.Party;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PartyAddress;
import org.dcsa.conformance.specifications.standards.dt.v100.model.PartyContactDetail;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PlaceOfDelivery;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PlaceOfIssue;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PlaceOfReceipt;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PortOfDischarge;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.PortOfLoading;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Reference;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ReferenceConsignmentItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Seal;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.Shipper;
import org.dcsa.conformance.specifications.standards.dt.v100.model.TareWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.TaxLegalReference;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.TransportDocument;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.Transports;
import org.dcsa.conformance.specifications.standards.dt.v100.model.UtilizedTransportEquipment;
import org.dcsa.conformance.specifications.standards.ebl.v300.model.VesselVoyage;

public class EBLStandardSpecification extends StandardSpecification {

  private final GetTransportDocumentEndpoint getTransportDocumentEndpoint;

  public EBLStandardSpecification() {
    super("Bill of Lading", "EBL", "3.0.0");

    openAPI.path(
        "/v3/transport-documents/{transportDocumentReference}",
        new PathItem().get(operationTransportDocumentGet()));

    getTransportDocumentEndpoint = new GetTransportDocumentEndpoint();
  }

  @Override
  protected Stream<Class<?>> modelClassesStream() {
    return Stream.of(
      ActiveReeferSettings.class,
      Address.class,
      CargoGrossVolume.class,
      CargoGrossWeight.class,
      CargoItem.class,
      CargoNetVolume.class,
      CargoNetWeight.class,
      CarriersAgentAtDestination.class,
      Charge.class,
      City.class,
      Consignee.class,
      ConsignmentItem.class,
      CustomsReference.class,
      DangerousGoods.class,
      DocumentParties.class,
      EmergencyContactDetails.class,
      Endorsee.class,
      Equipment.class,
      ExportLicense.class,
      Facility.class,
      GeoCoordinate.class,
      GrossWeight.class,
      IdentifyingCode.class,
      ImportLicense.class,
      InnerPackaging.class,
      InvoicePayableAt.class,
      IssuingParty.class,
      Limits.class,
      NationalCommodityCode.class,
      NetExplosiveContent.class,
      NetVolume.class,
      NetWeight.class,
      NotifyParty.class,
      OnwardInlandRouting.class,
      OtherDocumentParty.class,
      OuterPackaging.class,
      Party.class,
      PartyAddress.class,
      PartyContactDetail.class,
      PlaceOfDelivery.class,
      PlaceOfIssue.class,
      PlaceOfReceipt.class,
      PortOfDischarge.class,
      PortOfLoading.class,
      Reference.class,
      ReferenceConsignmentItem.class,
      Seal.class,
      Shipper.class,
      TareWeight.class,
      TaxLegalReference.class,
      TransportDocument.class,
      Transports.class,
      UtilizedTransportEquipment.class,
      VesselVoyage.class);
  }

  @Override
  protected List<String> getRootTypeNames() {
    return List.of(TransportDocument.class.getSimpleName());
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, List<List<String>>>
      getOldDataValuesBySheetClass() {
    // there's typically more than one entry in this map
    return Map.ofEntries(Map.entry(AttributesHierarchicalSheet.class, "attributes-hierarchical"))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    DataOverviewSheet.importFromString(
                            SpecificationToolkit.readLocalFile(
                                "./generated-resources/standards/an/v100/an-v1.0.0-data-overview-%s.csv"
                                    .formatted(entry.getValue())))
                        .stream()
                        .filter(row -> !row.getFirst().startsWith("ArrivalNoticeNotification"))
                        .toList()));
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, Map<String, String>>
      getChangedPrimaryKeyByOldPrimaryKeyBySheetClass() {
    return Map.ofEntries(
        Map.entry(
            AttributesHierarchicalSheet.class,
            Map.ofEntries(
                Map.entry("ArrivalNotice /", "TransportDocument /"),
                Map.entry("ArrivalNotice / issueDateTime", "TransportDocument / issueDate"),
                Map.entry("ArrivalNotice / transport /", "TransportDocument / transports /"))));
  }

  protected QueryParametersFilterEndpoint getQueryParametersFilterEndpoint() {
    return getTransportDocumentEndpoint;
  }

  @Override
  protected boolean swapOldAndNewInDataOverview() {
    return true;
  }

  private static Operation operationTransportDocumentGet() {
    return new Operation()
        .summary("Retrieves a transport document")
        .description("")
        .operationId("get-transport-document")
        .parameters(new GetTransportDocumentEndpoint().getQueryParameters())
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description(
                            "Transport document with the reference matching the URL parameter")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref(
                                                    SpecificationToolkit.getComponentSchema$ref(
                                                        TransportDocument.class)))))));
  }
}
