package org.dcsa.conformance.specifications.standards.an.v100;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.specifications.dataoverview.AttributesHierarchicalSheet;
import org.dcsa.conformance.specifications.dataoverview.AttributesNormalizedSheet;
import org.dcsa.conformance.specifications.dataoverview.DataOverviewSheet;
import org.dcsa.conformance.specifications.dataoverview.QueryFiltersSheet;
import org.dcsa.conformance.specifications.dataoverview.QueryParametersSheet;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.generator.StandardSpecification;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNotice;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNoticeNotification;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNoticeNotificationsMessage;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNoticesMessage;
import org.dcsa.conformance.specifications.standards.an.v100.model.ContactInformation;
import org.dcsa.conformance.specifications.standards.an.v100.model.DocumentParty;
import org.dcsa.conformance.specifications.standards.an.v100.model.EmbeddedDocument;
import org.dcsa.conformance.specifications.standards.an.v100.model.FreeTime;
import org.dcsa.conformance.specifications.standards.an.v100.model.IdentifyingPartyCode;
import org.dcsa.conformance.specifications.standards.an.v100.model.Location;
import org.dcsa.conformance.specifications.standards.an.v100.model.Reference;
import org.dcsa.conformance.specifications.standards.an.v100.model.TaxOrLegalReference;
import org.dcsa.conformance.specifications.standards.an.v100.model.Transport;
import org.dcsa.conformance.specifications.standards.an.v100.model.VesselVoyage;
import org.dcsa.conformance.specifications.standards.an.v100.model.Volume;
import org.dcsa.conformance.specifications.standards.an.v100.model.Weight;
import org.dcsa.conformance.specifications.standards.an.v100.types.CarrierClause;
import org.dcsa.conformance.specifications.standards.an.v100.types.ContainerLoadTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.CountryCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.CurrencyAmount;
import org.dcsa.conformance.specifications.standards.an.v100.types.CurrencyCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.CustomsReferenceValue;
import org.dcsa.conformance.specifications.standards.an.v100.types.DestinationDeliveryTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.DocumentPartyTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.FacilityCodeListProvider;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.standards.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.standards.an.v100.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.FreeTimeTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.HSCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.ImoPackagingCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.InhalationZoneTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.IsoEquipmentCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.ModeOfTransportCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.NationalCommodityCodeValue;
import org.dcsa.conformance.specifications.standards.an.v100.types.PartyCodeListProvider;
import org.dcsa.conformance.specifications.standards.an.v100.types.PaymentTermCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.PersonTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.SegregationGroupCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.SubsidiaryRisk;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportDocumentReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.TransportDocumentTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.UNLocationCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.UnecePackageCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.standards.an.v100.types.VesselIMONumber;
import org.dcsa.conformance.specifications.standards.an.v100.types.VesselVoyageTypeCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.VolumeUnitCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.WeightUnitCode;
import org.dcsa.conformance.specifications.standards.an.v100.types.WoodDeclarationTypeCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ActiveReeferSettings;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Address;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoGrossVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoGrossWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoNetVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CargoNetWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Charge;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ConsignmentItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.CustomsReference;
import org.dcsa.conformance.specifications.standards.dt.v100.model.DangerousGoods;
import org.dcsa.conformance.specifications.standards.dt.v100.model.EmergencyContactDetails;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Equipment;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ExportLicense;
import org.dcsa.conformance.specifications.standards.dt.v100.model.GrossWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ImportLicense;
import org.dcsa.conformance.specifications.standards.dt.v100.model.InnerPackaging;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Limits;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NationalCommodityCode;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetExplosiveContent;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetVolume;
import org.dcsa.conformance.specifications.standards.dt.v100.model.NetWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.OuterPackaging;
import org.dcsa.conformance.specifications.standards.dt.v100.model.ReferenceConsignmentItem;
import org.dcsa.conformance.specifications.standards.dt.v100.model.Seal;
import org.dcsa.conformance.specifications.standards.dt.v100.model.TareWeight;
import org.dcsa.conformance.specifications.standards.dt.v100.model.UtilizedTransportEquipment;

public class ANStandardSpecification extends StandardSpecification {

  public static final String TAG_ARRIVAL_NOTICE_PUBLISHERS = "AN Publisher Endpoints";
  public static final String TAG_ARRIVAL_NOTICE_SUBSCRIBERS = "AN Subscriber Endpoints";

  private final GetArrivalNoticesEndpoint getArrivalNoticesEndpoint;

  public ANStandardSpecification() {
    super("Arrival Notice", "AN", "1.0.0");

    openAPI.addTagsItem(
        new Tag()
            .name(TAG_ARRIVAL_NOTICE_PUBLISHERS)
            .description("Endpoints implemented by the adopters who publish arrival notices"));
    openAPI.addTagsItem(
        new Tag()
            .name(TAG_ARRIVAL_NOTICE_SUBSCRIBERS)
            .description("Endpoints implemented by the adopters who receive arrival notices"));

    openAPI.path(
        "/arrival-notices",
        new PathItem().get(operationArrivalNoticesGet()).put(operationArrivalNoticesPut()));
    openAPI.path(
        "/arrival-notice-notifications",
        new PathItem().put(operationArrivalNoticeNotificationsPut()));

    getArrivalNoticesEndpoint = new GetArrivalNoticesEndpoint();
  }

  @Override
  protected Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        // DT
        ActiveReeferSettings.class,
        Address.class,
        CargoGrossVolume.class,
        CargoGrossWeight.class,
        CargoItem.class,
        CargoNetVolume.class,
        CargoNetWeight.class,
        Charge.class,
        ConsignmentItem.class,
        CustomsReference.class,
        DangerousGoods.class,
        EmergencyContactDetails.class,
        Equipment.class,
        ExportLicense.class,
        GrossWeight.class,
        ImportLicense.class,
        InnerPackaging.class,
        Limits.class,
        NationalCommodityCode.class,
        NetExplosiveContent.class,
        NetVolume.class,
        NetWeight.class,
        OuterPackaging.class,
        ReferenceConsignmentItem.class,
        Seal.class,
        TareWeight.class,
        UtilizedTransportEquipment.class,
        // AN
        ArrivalNotice.class,
        ArrivalNoticeNotification.class,
        ArrivalNoticeNotificationsMessage.class,
        ArrivalNoticesMessage.class,
        CarrierClause.class,
        ContactInformation.class,
        ContainerLoadTypeCode.class,
        CountryCode.class,
        CurrencyAmount.class,
        CurrencyCode.class,
        CustomsReferenceValue.class,
        DestinationDeliveryTypeCode.class,
        DocumentParty.class,
        DocumentPartyTypeCode.class,
        EmbeddedDocument.class,
        EquipmentReference.class,
        FacilityCodeListProvider.class,
        FormattedDate.class,
        FormattedDateTime.class,
        FreeTime.class,
        FreeTimeTimeUnitCode.class,
        FreeTimeTypeCode.class,
        HSCode.class,
        IdentifyingPartyCode.class,
        ImoPackagingCode.class,
        InhalationZoneTypeCode.class,
        IsoEquipmentCode.class,
        Location.class,
        ModeOfTransportCode.class,
        NationalCommodityCodeValue.class,
        PartyCodeListProvider.class,
        PaymentTermCode.class,
        PersonTypeCode.class,
        Reference.class,
        SegregationGroupCode.class,
        SubsidiaryRisk.class,
        TaxOrLegalReference.class,
        Transport.class,
        TransportDocumentReference.class,
        TransportDocumentTypeCode.class,
        UniversalVoyageReference.class,
        UnecePackageCode.class,
        UNLocationCode.class,
        VesselIMONumber.class,
        VesselVoyage.class,
        VesselVoyageTypeCode.class,
        Volume.class,
        VolumeUnitCode.class,
        Weight.class,
        WeightUnitCode.class,
        WoodDeclarationTypeCode.class);
  }

  @Override
  protected List<String> getRootTypeNames() {
    return List.of(
        ArrivalNotice.class.getSimpleName(), ArrivalNoticeNotification.class.getSimpleName());
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, List<List<String>>>
      getOldDataValuesBySheetClass() {
    return Map.ofEntries(
            Map.entry(AttributesHierarchicalSheet.class, "attributes-hierarchical"),
            Map.entry(AttributesNormalizedSheet.class, "attributes-normalized"),
            Map.entry(QueryParametersSheet.class, "query-parameters"),
            Map.entry(QueryFiltersSheet.class, "query-filters"))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    DataOverviewSheet.importFromString(
                        SpecificationToolkit.readRemoteFile(
                            "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/bd25914e5526aa7c8a67aba8a28010555c82d1ad/specifications/generated-resources/an-v1.0.0-data-overview-%s.csv"
                                .formatted(entry.getValue())))));
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, Map<String, String>>
      getChangedPrimaryKeyByOldPrimaryKeyBySheetClass() {
    return Map.ofEntries(
        Map.entry(
            AttributesHierarchicalSheet.class,
            Map.ofEntries(
                Map.entry(
                    "ArrivalNotice / consignmentItems / cargoItems / outerPackaging / dangerousGoods / codedVariantList",
                    "ArrivalNotice / consignmentItems / cargoItems / outerPackaging / dangerousGoods / codedVariant"),
                Map.entry(
                    "ArrivalNotice / consignmentItems / customsReferences / crType",
                    "ArrivalNotice / consignmentItems / customsReferences / typeCode"),
                Map.entry(
                    "ArrivalNotice / consignmentItems / customsReferences / values",
                    "ArrivalNotice / consignmentItems / customsReferences / referenceValues"),
                Map.entry(
                    "ArrivalNotice / consignmentItems / nationalCommodityCodes / nccType",
                    "ArrivalNotice / consignmentItems / nationalCommodityCodes / typeCode"),
                Map.entry(
                    "ArrivalNotice / consignmentItems / nationalCommodityCodes / nccValues",
                    "ArrivalNotice / consignmentItems / nationalCommodityCodes / codeValues"),
                Map.entry("ArrivalNotice / issueDate", "ArrivalNotice / issueDateTime"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage", "ArrivalNotice / transport / vesselVoyages"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / carrierImportVoyageNumber",
                    "ArrivalNotice / transport / vesselVoyages / carrierVoyageNumber"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / typeCode",
                    "ArrivalNotice / transport / vesselVoyages / typeCode"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / universalImportVoyageReference",
                    "ArrivalNotice / transport / vesselVoyages / universalVoyageReference"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / vesselFlag",
                    "ArrivalNotice / transport / vesselVoyages / vesselFlag"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / vesselIMONumber",
                    "ArrivalNotice / transport / vesselVoyages / vesselIMONumber"),
                Map.entry(
                    "ArrivalNotice / vesselVoyage / vesselName",
                    "ArrivalNotice / transport / vesselVoyages / vesselName"))),
        Map.entry(
            AttributesNormalizedSheet.class,
            Map.ofEntries(
                Map.entry("ArrivalNotice,issueDate", "ArrivalNotice,issueDateTime"),
                Map.entry("CustomsReference,crType", "CustomsReference,typeCode"),
                Map.entry("CustomsReference,values", "CustomsReference,referenceValues"),
                Map.entry("DangerousGoods,codedVariantList", "DangerousGoods,codedVariant"),
                Map.entry("NationalCommodityCode,nccType", "NationalCommodityCode,typeCode"),
                Map.entry("NationalCommodityCode,nccValues", "NationalCommodityCode,codeValues"),
                Map.entry(
                    "VesselVoyage,carrierImportVoyageNumber", "VesselVoyage,carrierVoyageNumber"),
                Map.entry(
                    "VesselVoyage,universalImportVoyageReference",
                    "VesselVoyage,universalVoyageReference"))),
        Map.entry(QueryFiltersSheet.class, Map.ofEntries()),
        Map.entry(QueryParametersSheet.class, Map.ofEntries()));
  }

  protected QueryParametersFilterEndpoint getQueryParametersFilterEndpoint() {
    return getArrivalNoticesEndpoint;
  }

  @Override
  protected boolean swapOldAndNewInDataOverview() {
    return false;
  }

  private static Operation operationArrivalNoticesGet() {
    return new Operation()
        .summary("Retrieves a list of arrival notices")
        .description("")
        .operationId("get-arrival-notices")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_PUBLISHERS))
        .parameters(new GetArrivalNoticesEndpoint().getQueryParameters())
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("List of arrival notices matching the query parameters")
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
                                                        ArrivalNoticesMessage.class)))))));
  }

  private static Operation operationArrivalNoticesPut() {
    return new Operation()
        .summary("Sends a list of arrival notices")
        .description("")
        .operationId("put-arrival-notices")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_SUBSCRIBERS))
        .requestBody(
            new RequestBody()
                .description("List of arrival notices")
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/json",
                            new MediaType()
                                .schema(
                                    new Schema<>()
                                        .$ref(
                                            SpecificationToolkit.getComponentSchema$ref(
                                                ArrivalNoticesMessage.class))))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "204",
                    new ApiResponse()
                        .description("Empty response")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))));
  }

  private static Operation operationArrivalNoticeNotificationsPut() {
    return new Operation()
        .summary("Sends a list of arrival notice lightweight notifications")
        .description("")
        .operationId("put-arrival-notice-notifications")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_SUBSCRIBERS))
        .requestBody(
            new RequestBody()
                .description("List of arrival notice lightweight notifications")
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/json",
                            new MediaType()
                                .schema(
                                    new Schema<>()
                                        .$ref(
                                            SpecificationToolkit.getComponentSchema$ref(
                                                ArrivalNoticeNotificationsMessage.class))))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "204",
                    new ApiResponse()
                        .description("Empty response")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))));
  }
}
