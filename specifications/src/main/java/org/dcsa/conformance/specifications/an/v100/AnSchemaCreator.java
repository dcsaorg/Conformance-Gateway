package org.dcsa.conformance.specifications.an.v100;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.specifications.an.v100.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.an.v100.dataoverview.DataOverview;
import org.dcsa.conformance.specifications.an.v100.model.ActiveReeferSettings;
import org.dcsa.conformance.specifications.an.v100.model.Address;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNotice;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNoticeNotification;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNoticeNotificationsMessage;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNoticesMessage;
import org.dcsa.conformance.specifications.an.v100.model.CargoItem;
import org.dcsa.conformance.specifications.an.v100.model.Charge;
import org.dcsa.conformance.specifications.an.v100.model.ConsignmentItem;
import org.dcsa.conformance.specifications.an.v100.model.ContactInformation;
import org.dcsa.conformance.specifications.an.v100.model.CustomsReference;
import org.dcsa.conformance.specifications.an.v100.model.DangerousGoods;
import org.dcsa.conformance.specifications.an.v100.model.DocumentParty;
import org.dcsa.conformance.specifications.an.v100.model.EmbeddedDocument;
import org.dcsa.conformance.specifications.an.v100.model.EmergencyContactDetails;
import org.dcsa.conformance.specifications.an.v100.model.Equipment;
import org.dcsa.conformance.specifications.an.v100.model.FreeTime;
import org.dcsa.conformance.specifications.an.v100.model.IdentifyingPartyCode;
import org.dcsa.conformance.specifications.an.v100.model.InnerPackaging;
import org.dcsa.conformance.specifications.an.v100.model.Location;
import org.dcsa.conformance.specifications.an.v100.model.NationalCommodityCode;
import org.dcsa.conformance.specifications.an.v100.model.OuterPackaging;
import org.dcsa.conformance.specifications.an.v100.model.Reference;
import org.dcsa.conformance.specifications.an.v100.model.Seal;
import org.dcsa.conformance.specifications.an.v100.model.TaxOrLegalReference;
import org.dcsa.conformance.specifications.an.v100.model.TemperatureLimits;
import org.dcsa.conformance.specifications.an.v100.model.Transport;
import org.dcsa.conformance.specifications.an.v100.model.UtilizedTransportEquipment;
import org.dcsa.conformance.specifications.an.v100.model.VesselVoyage;
import org.dcsa.conformance.specifications.an.v100.model.Volume;
import org.dcsa.conformance.specifications.an.v100.model.Weight;
import org.dcsa.conformance.specifications.an.v100.types.AirExchangeUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.CargoDescriptionLine;
import org.dcsa.conformance.specifications.an.v100.types.CarrierClause;
import org.dcsa.conformance.specifications.an.v100.types.ContainerLoadTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.CountryCode;
import org.dcsa.conformance.specifications.an.v100.types.CurrencyAmount;
import org.dcsa.conformance.specifications.an.v100.types.CurrencyCode;
import org.dcsa.conformance.specifications.an.v100.types.CustomsReferenceValue;
import org.dcsa.conformance.specifications.an.v100.types.DestinationDeliveryTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.DocumentPartyTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.EquipmentReference;
import org.dcsa.conformance.specifications.an.v100.types.FacilityCodeListProvider;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDate;
import org.dcsa.conformance.specifications.an.v100.types.FormattedDateTime;
import org.dcsa.conformance.specifications.an.v100.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.FreeTimeTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.HSCode;
import org.dcsa.conformance.specifications.an.v100.types.ImoPackagingCode;
import org.dcsa.conformance.specifications.an.v100.types.InhalationZoneTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.IsoEquipmentCode;
import org.dcsa.conformance.specifications.an.v100.types.NationalCommodityCodeValue;
import org.dcsa.conformance.specifications.an.v100.types.PartyCodeListProvider;
import org.dcsa.conformance.specifications.an.v100.types.PaymentTermCode;
import org.dcsa.conformance.specifications.an.v100.types.PersonTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.SealSourceCode;
import org.dcsa.conformance.specifications.an.v100.types.SegregationGroupCode;
import org.dcsa.conformance.specifications.an.v100.types.ShippingMark;
import org.dcsa.conformance.specifications.an.v100.types.SubsidiaryRisk;
import org.dcsa.conformance.specifications.an.v100.types.TemperatureUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.TransportDocumentReference;
import org.dcsa.conformance.specifications.an.v100.types.TransportDocumentTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.UNLocationCode;
import org.dcsa.conformance.specifications.an.v100.types.UnecePackageCode;
import org.dcsa.conformance.specifications.an.v100.types.UniversalVoyageReference;
import org.dcsa.conformance.specifications.an.v100.types.VesselIMONumber;
import org.dcsa.conformance.specifications.an.v100.types.VesselVoyageDestinationTypeCode;
import org.dcsa.conformance.specifications.an.v100.types.VolumeUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.WeightUnitCode;
import org.dcsa.conformance.specifications.an.v100.types.WoodDeclarationTypeCode;

@Slf4j
public class AnSchemaCreator {

  public static final String TAG_ARRIVAL_NOTICE_PUBLISHERS = "AN Publisher Endpoints";
  public static final String TAG_ARRIVAL_NOTICE_SUBSCRIBERS = "AN Subscriber Endpoints";

  public static final Map<String, Map<String, List<SchemaConstraint>>> constraintsByClassAndField =
      new HashMap<>();

  static {
    AnSchemaCreator.modelClassesStream()
        .forEach(
            modelClass ->
                OpenApiToolkit.getClassConstraints(modelClass.getName())
                    .forEach(
                        schemaConstraint ->
                            schemaConstraint
                                .getTargetFields()
                                .forEach(
                                    targetField ->
                                        constraintsByClassAndField
                                            .computeIfAbsent(
                                                modelClass.getSimpleName(),
                                                ignoredClassName -> new HashMap<>())
                                            .computeIfAbsent(
                                                targetField.getName(),
                                                ignoredFieldName -> new ArrayList<>())
                                            .add(schemaConstraint))));
  }

  @SneakyThrows
  public static void createSchema() {
    OpenAPI openAPI =
        new OpenAPI()
            .openapi("3.0.3")
            .info(
                new Info()
                    .version("1.0.0")
                    .title("DCSA Arrival Notice API")
                    .description(
                        readResourceFile("conformance/specifications/an/v100/openapi-root.md"))
                    .license(
                        new License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                    .contact(
                        new io.swagger.v3.oas.models.info.Contact()
                            .name("Digital Container Shipping Association (DCSA)")
                            .url("https://dcsa.org")
                            .email("info@dcsa.org")))
            .addTagsItem(
                new Tag()
                    .name(TAG_ARRIVAL_NOTICE_PUBLISHERS)
                    .description(
                        "Endpoints implemented by the adopters who publish arrival notices"))
            .addTagsItem(
                new Tag()
                    .name(TAG_ARRIVAL_NOTICE_SUBSCRIBERS)
                    .description(
                        "Endpoints implemented by the adopters who receive arrival notices"));

    // Extract and register Java class schemas: add Parameters, Headers, and Schemas.
    Components components = new Components();
    ModelConverters.getInstance().addConverter(new ModelValidatorConverter());
    modelClassesStream()
        .forEach(
            modelClass ->
                ModelConverters.getInstance().read(modelClass).forEach(components::addSchemas));

    components.addHeaders(
        "API-Version",
        new Header()
            .description(
                "SemVer used to indicate the version of the contract (API version) returned.")
            .schema(new Schema<>().type("string").example("1.0.0")));

    openAPI.setComponents(components);

    openAPI.path(
        "/arrival-notices",
        new PathItem().get(operationArrivalNoticesGet()).put(operationArrivalNoticesPut()));
    openAPI.path(
        "/arrival-notice-notifications",
        new PathItem().put(operationArrivalNoticeNotificationsPut()));

    YAMLFactory yamlFactory =
        YAMLFactory.builder()
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
            .configure(YAMLGenerator.Feature.SPLIT_LINES, false)
            .build();

    ObjectMapper mapper = new ObjectMapper(yamlFactory);
    mapper.findAndRegisterModules();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
    // Prevent date-time example values getting converted to unix timestamps.
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.addMixIn(Schema.class, SchemaMixin.class);
    mapper.addMixIn(Object.class, ValueSetFlagIgnoreMixin.class); // Remove valueSetFlag attribute.

    String yamlContent = mapper.writeValueAsString(openAPI);
    String exportFileDir = "./generated-resources/";
    String yamlFilePath = exportFileDir + "an-v1.0.0-openapi.yaml";
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    log.info("OpenAPI spec exported to {}", yamlFilePath);

    GetArrivalNoticesEndpoint getArrivalNoticesEndpoint = new GetArrivalNoticesEndpoint();
    String excelFilePath = exportFileDir + "an-v1.0.0-data-overview.xlsx";
    DataOverview dataOverview =
        new DataOverview(
            openAPI,
            getArrivalNoticesEndpoint.getQueryParameters(),
            getArrivalNoticesEndpoint.getRequiredAndOptionalFilters());
    dataOverview.exportToExcelFile(excelFilePath);
    dataOverview.exportToCsvFiles(exportFileDir + "an-v1.0.0-data-overview-%s.csv");
  }

  private static Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        ActiveReeferSettings.class,
        Address.class,
        AirExchangeUnitCode.class,
        ArrivalNotice.class,
        ArrivalNoticeNotification.class,
        ArrivalNoticeNotificationsMessage.class,
        ArrivalNoticesMessage.class,
        CargoDescriptionLine.class,
        CargoItem.class,
        CarrierClause.class,
        Charge.class,
        ConsignmentItem.class,
        ContactInformation.class,
        ContainerLoadTypeCode.class,
        CountryCode.class,
        CurrencyAmount.class,
        CurrencyCode.class,
        CustomsReference.class,
        CustomsReferenceValue.class,
        DangerousGoods.class,
        DestinationDeliveryTypeCode.class,
        DocumentParty.class,
        DocumentPartyTypeCode.class,
        EmbeddedDocument.class,
        EmergencyContactDetails.class,
        Equipment.class,
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
        InnerPackaging.class,
        IsoEquipmentCode.class,
        Location.class,
        NationalCommodityCode.class,
        NationalCommodityCodeValue.class,
        OuterPackaging.class,
        PartyCodeListProvider.class,
        PaymentTermCode.class,
        PersonTypeCode.class,
        Reference.class,
        Seal.class,
        SealSourceCode.class,
        SegregationGroupCode.class,
        ShippingMark.class,
        SubsidiaryRisk.class,
        TaxOrLegalReference.class,
        TemperatureLimits.class,
        TemperatureUnitCode.class,
        Transport.class,
        TransportDocumentReference.class,
        TransportDocumentTypeCode.class,
        UniversalVoyageReference.class,
        UnecePackageCode.class,
        UNLocationCode.class,
        UtilizedTransportEquipment.class,
        VesselIMONumber.class,
        VesselVoyage.class,
        VesselVoyageDestinationTypeCode.class,
        Volume.class,
        VolumeUnitCode.class,
        Weight.class,
        WeightUnitCode.class,
        WoodDeclarationTypeCode.class);
  }

  static String readResourceFile(@SuppressWarnings("SameParameterValue") String fileName) {
    try {
      return Files.readString(
          Paths.get(
              Objects.requireNonNull(AnSchemaCreator.class.getClassLoader().getResource(fileName))
                  .toURI()));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to read file from resources: " + fileName, e);
    }
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
                                                    getComponentSchema$ref(
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
                                            getComponentSchema$ref(ArrivalNoticesMessage.class))))))
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
                                            getComponentSchema$ref(
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

  private static String getComponentSchema$ref(Class<?> schemaClass) {
    return "#/components/schemas/%s".formatted(schemaClass.getSimpleName());
  }
}
