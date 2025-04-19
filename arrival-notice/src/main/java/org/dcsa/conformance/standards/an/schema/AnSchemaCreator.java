package org.dcsa.conformance.standards.an.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
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
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.standards.an.schema.model.ActiveReeferSettings;
import org.dcsa.conformance.standards.an.schema.model.Address;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNotice;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticeDigest;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticeDigestsMessage;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticesMessage;
import org.dcsa.conformance.standards.an.schema.model.CargoItem;
import org.dcsa.conformance.standards.an.schema.model.CarrierClause;
import org.dcsa.conformance.standards.an.schema.model.Charge;
import org.dcsa.conformance.standards.an.schema.model.ConsignmentItem;
import org.dcsa.conformance.standards.an.schema.model.ContactInformation;
import org.dcsa.conformance.standards.an.schema.model.CustomsReference;
import org.dcsa.conformance.standards.an.schema.model.DangerousGoods;
import org.dcsa.conformance.standards.an.schema.model.DocumentParty;
import org.dcsa.conformance.standards.an.schema.model.EmergencyContactDetails;
import org.dcsa.conformance.standards.an.schema.model.Equipment;
import org.dcsa.conformance.standards.an.schema.model.FreeTime;
import org.dcsa.conformance.standards.an.schema.model.IdentifyingPartyCode;
import org.dcsa.conformance.standards.an.schema.model.InnerPackaging;
import org.dcsa.conformance.standards.an.schema.model.Location;
import org.dcsa.conformance.standards.an.schema.types.NationalCommodityCode;
import org.dcsa.conformance.standards.an.schema.model.OuterPackaging;
import org.dcsa.conformance.standards.an.schema.model.Reference;
import org.dcsa.conformance.standards.an.schema.model.Seal;
import org.dcsa.conformance.standards.an.schema.model.TaxOrLegalReference;
import org.dcsa.conformance.standards.an.schema.model.TemperatureLimits;
import org.dcsa.conformance.standards.an.schema.model.Transport;
import org.dcsa.conformance.standards.an.schema.model.UtilizedTransportEquipment;
import org.dcsa.conformance.standards.an.schema.model.VesselVoyage;
import org.dcsa.conformance.standards.an.schema.model.Volume;
import org.dcsa.conformance.standards.an.schema.model.Weight;
import org.dcsa.conformance.standards.an.schema.types.AirExchangeUnitCode;
import org.dcsa.conformance.standards.an.schema.types.CargoDescriptionLine;
import org.dcsa.conformance.standards.an.schema.types.ContainerLoadTypeCode;
import org.dcsa.conformance.standards.an.schema.types.CountryCode;
import org.dcsa.conformance.standards.an.schema.types.CurrencyAmount;
import org.dcsa.conformance.standards.an.schema.types.CurrencyCode;
import org.dcsa.conformance.standards.an.schema.types.CustomsReferenceValue;
import org.dcsa.conformance.standards.an.schema.types.DestinationDeliveryTypeCode;
import org.dcsa.conformance.standards.an.schema.types.DocumentPartyTypeCode;
import org.dcsa.conformance.standards.an.schema.types.FacilityCodeListProvider;
import org.dcsa.conformance.standards.an.schema.types.FreeTimeTimeUnitCode;
import org.dcsa.conformance.standards.an.schema.types.FreeTimeTypeCode;
import org.dcsa.conformance.standards.an.schema.types.HSCode;
import org.dcsa.conformance.standards.an.schema.types.ISOEquipmentCode;
import org.dcsa.conformance.standards.an.schema.types.ModeOfTransportCode;
import org.dcsa.conformance.standards.an.schema.types.NationalCommodityCodeValue;
import org.dcsa.conformance.standards.an.schema.types.PartyCodeListProvider;
import org.dcsa.conformance.standards.an.schema.types.PaymentTermCode;
import org.dcsa.conformance.standards.an.schema.types.PersonTypeCode;
import org.dcsa.conformance.standards.an.schema.types.SealSourceCode;
import org.dcsa.conformance.standards.an.schema.types.ShippingMark;
import org.dcsa.conformance.standards.an.schema.types.TemperatureUnitCode;
import org.dcsa.conformance.standards.an.schema.types.TransportDocumentTypeCode;
import org.dcsa.conformance.standards.an.schema.types.UNLocationCode;
import org.dcsa.conformance.standards.an.schema.types.UniversalVoyageReference;
import org.dcsa.conformance.standards.an.schema.types.VesselIMONumber;
import org.dcsa.conformance.standards.an.schema.types.VesselVoyageDestinationTypeCode;
import org.dcsa.conformance.standards.an.schema.types.WeightUnitCode;

@Slf4j
public class AnSchemaCreator {

  public static final String TAG_ARRIVAL_NOTICE_PUBLISHERS = "AN Publisher Endpoints";
  public static final String TAG_ARRIVAL_NOTICE_SUBSCRIBERS = "AN Subscriber Endpoints";

  @SneakyThrows
  public static void createSchema() {
    OpenAPI openAPI =
        new OpenAPI()
            .openapi("3.0.3")
            .info(
                new Info()
                    .version("1.0.0")
                    .title("DCSA Arrival Notice API")
                    .description(readResourceFile("standards/an/schemas/an-schema-description.md"))
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
                        "Endpoints implemented by the adopters who publish Arrival Notices"))
            .addTagsItem(
                new Tag()
                    .name(TAG_ARRIVAL_NOTICE_SUBSCRIBERS)
                    .description(
                        "Endpoints implemented by the adopters who receive Arrival Notices"));

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
    openAPI.path("/arrival-notice-digests", new PathItem().put(operationArrivalNoticeDigestsPut()));

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
    String exportFileDir = "../arrival-notice/src/main/resources/standards/an/schemas/";
    String yamlFilePath = exportFileDir + "an-v1.0.0-openapi.yaml";
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    log.info("OpenAPI spec exported to {}", yamlFilePath);

    exportDataOverviewCsv(openAPI, exportFileDir + "an-v1.0.0-data-overview.csv");
  }

  private static void exportDataOverviewCsv(OpenAPI openApi, String csvFilePath)
      throws IOException {
    Map<String, Schema<?>> schemas =
        OpenApiToolkit.parameterizeStringRawSchemaMap(openApi.getComponents().getSchemas());
    CsvMapper csvMapper = new CsvMapper();
    String objectColumnTitle = "Object";
    String attributeColumnTitle = "Attribute";
    String typeColumnTitle = "Type";
    String requiredColumnTitle = "Required";
    String sizeColumnTitle = "Size";
    String patternColumnTitle = "Pattern";
    String exampleColumnTitle = "Example";
    String descriptionColumnTitle = "Description";
    CsvSchema csvSchema =
        CsvSchema.builder()
            .addColumn(objectColumnTitle)
            .addColumn(attributeColumnTitle)
            .addColumn(typeColumnTitle)
            .addColumn(requiredColumnTitle)
            .addColumn(sizeColumnTitle)
            .addColumn(patternColumnTitle)
            .addColumn(exampleColumnTitle)
            .addColumn(descriptionColumnTitle)
            .build()
            .withHeader();
    ObjectWriter csvWriter = csvMapper.writer(csvSchema);
    List<Map<String, Object>> csvRows = new ArrayList<>();
    new TreeSet<>(schemas.keySet())
        .forEach(
            typeName -> {
              Schema<?> typeSchema = schemas.get(typeName);
              Set<String> requiredAttributes =
                  new HashSet<>(
                      Objects.requireNonNullElse(typeSchema.getRequired(), Collections.emptySet()));
              Map<String, Schema<?>> typeAttributeProperties =
                  OpenApiToolkit.parameterizeStringRawSchemaMap(typeSchema.getProperties());
              new TreeSet<>(typeAttributeProperties.keySet())
                  .forEach(
                      attributeName -> {
                        Schema<?> attributeSchema = typeAttributeProperties.get(attributeName);
                        String attributeSchemaType = attributeSchema.getType();
                        String csvType = "UNKNOWN";
                        String csvRequired = requiredAttributes.remove(attributeName) ? "yes" : "";
                        Integer maxLength = attributeSchema.getMaxLength();
                        String csvSize =
                            maxLength == null || maxLength == Integer.MAX_VALUE
                                ? ""
                                : maxLength.toString(); // FIXME range, array items
                        switch (attributeSchemaType) {
                          case "array":
                            {
                              Schema<?> itemSchema = attributeSchema.getItems();
                              String itemType = itemSchema.getType();
                              if (itemType == null) {
                                String $ref = itemSchema.get$ref();
                                if ($ref != null) {
                                  itemType = $ref.substring("#/components/schemas/".length());
                                } else {
                                  itemType = "UNKNOWN";
                                }
                              }
                              csvType = "%s list".formatted(itemType);
                              break;
                            }
                          case "object":
                            {
                              List<Schema<?>> allOf =
                                  OpenApiToolkit.parameterizeRawSchemaList(
                                      attributeSchema.getAllOf());
                              if (allOf.size() == 1) {
                                String $ref = allOf.getFirst().get$ref();
                                if ($ref != null) {
                                  csvType = $ref.substring("#/components/schemas/".length());
                                }
                              }
                              break;
                            }
                          case null:
                            {
                              String $ref = attributeSchema.get$ref();
                              if ($ref != null) {
                                csvType = $ref.substring("#/components/schemas/".length());
                              } else {
                                csvType = "string";
                              }
                              break;
                            }
                          default:
                            {
                              csvType = attributeSchemaType;
                              break;
                            }
                        }
                        String csvExample =
                            Objects.requireNonNullElse(attributeSchema.getExample(), "").toString();
                        String csvDescription =
                            Objects.requireNonNullElse(attributeSchema.getDescription(), "");
                        String csvPattern = "";
                        if (csvType.equals("string")) {
                          String schemaPattern = attributeSchema.getPattern();
                          if (schemaPattern != null) {
                            csvPattern = schemaPattern;
                          }
                        }
                        csvRows.add(
                            Map.ofEntries(
                                Map.entry(objectColumnTitle, typeName),
                                Map.entry(attributeColumnTitle, attributeName),
                                Map.entry(typeColumnTitle, csvType),
                                Map.entry(requiredColumnTitle, csvRequired),
                                Map.entry(sizeColumnTitle, csvSize),
                                Map.entry(patternColumnTitle, csvPattern),
                                Map.entry(exampleColumnTitle, csvExample),
                                Map.entry(descriptionColumnTitle, csvDescription)));
                      });
            });
    StringWriter stringWriter = new StringWriter();
    csvWriter.writeValue(stringWriter, csvRows);
    Files.writeString(Paths.get(csvFilePath), stringWriter.toString());
    log.info("Data overview exported to {}", csvFilePath);
  }

  private static Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        ActiveReeferSettings.class,
        Address.class,
        AirExchangeUnitCode.class,
        ArrivalNotice.class,
        ArrivalNoticeDigest.class,
        ArrivalNoticeDigestsMessage.class,
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
        EmergencyContactDetails.class,
        Equipment.class,
        FacilityCodeListProvider.class,
        FreeTime.class,
        FreeTimeTimeUnitCode.class,
        FreeTimeTypeCode.class,
        HSCode.class,
        IdentifyingPartyCode.class,
        InnerPackaging.class,
        ISOEquipmentCode.class,
        Location.class,
        NationalCommodityCode.class,
        NationalCommodityCodeValue.class,
        ModeOfTransportCode.class,
        OuterPackaging.class,
        PartyCodeListProvider.class,
        PaymentTermCode.class,
        PersonTypeCode.class,
        Reference.class,
        Seal.class,
        SealSourceCode.class,
        ShippingMark.class,
        TaxOrLegalReference.class,
        TemperatureLimits.class,
        TemperatureUnitCode.class,
        Transport.class,
        TransportDocumentTypeCode.class,
        UniversalVoyageReference.class,
        UNLocationCode.class,
        UtilizedTransportEquipment.class,
        VesselIMONumber.class,
        VesselVoyage.class,
        VesselVoyageDestinationTypeCode.class,
        Volume.class,
        Weight.class,
        WeightUnitCode.class);
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
        .summary("Retrieves a list of Arrival Notices")
        .description("TODO endpoint description")
        .operationId("get-arrival-notices")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_PUBLISHERS))
        .parameters(
            List.of(
                new Parameter()
                    .in("query")
                    .name("transportDocumentReference")
                    .description(
                        "Reference of the transport document for which to return the associated Arrival Notices")
                    .example("TDR0123456")
                    .schema(new Schema<String>().type("string")),
                new Parameter()
                    .in("query")
                    .name("equipmentReference")
                    .description(
                        "Reference(s) of the equipment for which to return the associated Arrival Notices")
                    .example("APZU4812090,APZU4812091")
                    .schema(stringListQueryParameterSchema()),
                new Parameter()
                    .in("query")
                    .name("portOfDischarge")
                    .description(
                        "UN location of the port of discharge for which to retrieve available Arrival Notices")
                    .example("NLRTM")
                    .schema(new Schema<String>().type("string")),
                new Parameter()
                    .in("query")
                    .name("vesselIMONumber")
                    .description(
                        "IMO number of the vessel for which to retrieve available Arrival Notices")
                    .example("12345678")
                    .schema(new Schema<String>().type("string")),
                new Parameter()
                    .in("query")
                    .name("minEtaAtPortOfDischargeDate")
                    .description(
                        "Retrieve Arrival Notices with an ETA at port of discharge on or after this date")
                    .example("2025-01-23")
                    .schema(new Schema<String>().type("string").format("date")),
                new Parameter()
                    .in("query")
                    .name("maxEtaAtPortOfDischargeDate")
                    .description(
                        "Retrieve Arrival Notices with an ETA at port of discharge on or before this date")
                    .example("2025-01-23")
                    .schema(new Schema<String>().type("string").format("date")),
                new Parameter()
                    .in("query")
                    .name("includeCharges")
                    .description(
                        "Flag indicating whether to include Arrival Notice charges (default: true).")
                    .example(true)
                    .schema(new Schema<Boolean>().type("boolean"))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("TODO response description")
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
                                                    "#/components/schemas/ArrivalNoticesMessage"))))));
  }

  @SuppressWarnings("unchecked")
  private static Schema<List<String>> stringListQueryParameterSchema() {
    return new Schema<List<String>>().type("array").items(new Schema<String>().type("string"));
  }

  private static Operation operationArrivalNoticesPut() {
    return new Operation()
        .summary("Sends a list of Arrival Notices")
        .description("TODO description")
        .operationId("put-arrival-notices")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_SUBSCRIBERS))
        .requestBody(
            new RequestBody()
                .description("TODO request description")
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/json",
                            new MediaType()
                                .schema(
                                    new Schema<>()
                                        .$ref("#/components/schemas/ArrivalNoticesMessage")))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "204",
                    new ApiResponse()
                        .description("TODO response description")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))));
  }

  private static Operation operationArrivalNoticeDigestsPut() {
    return new Operation()
        .summary("Sends a list of Arrival Notice digests")
        .description("TODO description")
        .operationId("put-arrival-notice-digests")
        .tags(Collections.singletonList(TAG_ARRIVAL_NOTICE_SUBSCRIBERS))
        .requestBody(
            new RequestBody()
                .description("TODO request description")
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/json",
                            new MediaType()
                                .schema(
                                    new Schema<>()
                                        .$ref(
                                            "#/components/schemas/ArrivalNoticeDigestsMessage")))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "204",
                    new ApiResponse()
                        .description("TODO response description")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))));
  }
}
