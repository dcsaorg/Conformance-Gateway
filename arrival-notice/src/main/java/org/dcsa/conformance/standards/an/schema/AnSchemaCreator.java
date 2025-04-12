package org.dcsa.conformance.standards.an.schema;

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
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.SneakyThrows;
import org.dcsa.conformance.standards.an.schema.model.ActiveReeferSettings;
import org.dcsa.conformance.standards.an.schema.model.Address;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNotice;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticeDigest;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticeDigestsMessage;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNoticesMessage;
import org.dcsa.conformance.standards.an.schema.model.CargoItem;
import org.dcsa.conformance.standards.an.schema.model.Charge;
import org.dcsa.conformance.standards.an.schema.model.ConsignmentItem;
import org.dcsa.conformance.standards.an.schema.model.ContactInformation;
import org.dcsa.conformance.standards.an.schema.model.CustomsReference;
import org.dcsa.conformance.standards.an.schema.model.DangerousGoods;
import org.dcsa.conformance.standards.an.schema.model.DocumentParties;
import org.dcsa.conformance.standards.an.schema.model.DocumentParty;
import org.dcsa.conformance.standards.an.schema.model.EmergencyContactDetails;
import org.dcsa.conformance.standards.an.schema.model.Equipment;
import org.dcsa.conformance.standards.an.schema.model.FreeTime;
import org.dcsa.conformance.standards.an.schema.model.IdentifyingPartyCode;
import org.dcsa.conformance.standards.an.schema.model.InnerPackaging;
import org.dcsa.conformance.standards.an.schema.model.InvoicePayableAt;
import org.dcsa.conformance.standards.an.schema.model.Location;
import org.dcsa.conformance.standards.an.schema.model.NationalCommodityCode;
import org.dcsa.conformance.standards.an.schema.model.OuterPackaging;
import org.dcsa.conformance.standards.an.schema.model.Reference;
import org.dcsa.conformance.standards.an.schema.model.Seal;
import org.dcsa.conformance.standards.an.schema.model.TemperatureLimits;
import org.dcsa.conformance.standards.an.schema.model.Volume;
import org.dcsa.conformance.standards.an.schema.model.Weight;
import org.dcsa.conformance.standards.an.schema.model.TaxOrLegalReference;
import org.dcsa.conformance.standards.an.schema.model.Transport;
import org.dcsa.conformance.standards.an.schema.model.UtilizedTransportEquipment;
import org.dcsa.conformance.standards.an.schema.model.VesselVoyage;
import org.dcsa.conformance.standards.an.schema.types.FacilityCodeListProvider;
import org.dcsa.conformance.standards.an.schema.types.UNLocationCode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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
    Stream.of(
            ActiveReeferSettings.class,
            Address.class,
            ArrivalNotice.class,
            ArrivalNoticeDigest.class,
            ArrivalNoticeDigestsMessage.class,
            ArrivalNoticesMessage.class,
            CargoItem.class,
            Charge.class,
            ConsignmentItem.class,
            ContactInformation.class,
            CustomsReference.class,
            DangerousGoods.class,
            DocumentParty.class,
            DocumentParties.class,
            EmergencyContactDetails.class,
            Equipment.class,
            FacilityCodeListProvider.class,
            FreeTime.class,
            IdentifyingPartyCode.class,
            InnerPackaging.class,
            InvoicePayableAt.class,
            Location.class,
            NationalCommodityCode.class,
            OuterPackaging.class,
            Reference.class,
            Seal.class,
            TaxOrLegalReference.class,
            TemperatureLimits.class,
            Transport.class,
            UNLocationCode.class,
            UtilizedTransportEquipment.class,
            VesselVoyage.class,
            Volume.class,
            Weight.class)
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
    String yamlFilePath =
        "../arrival-notice/src/main/resources/standards/an/schemas/exported-AN_v1.0.0.yaml";
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    System.out.printf("OpenAPI spec exported to %s%n", yamlFilePath);
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
