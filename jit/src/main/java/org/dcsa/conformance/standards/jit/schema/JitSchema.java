package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.dcsa.conformance.standards.jit.schema.endpoints.PortCallEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.PortCallServicesEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.TerminalCallEndPoint;
import org.dcsa.conformance.standards.jit.schema.model.DetailedError;
import org.dcsa.conformance.standards.jit.schema.model.ErrorResponse;
import org.dcsa.conformance.standards.jit.schema.model.PortCall;
import org.dcsa.conformance.standards.jit.schema.model.PortCallService;
import org.dcsa.conformance.standards.jit.schema.model.TerminalCall;
import org.dcsa.conformance.standards.jit.schema.model.Vessel;

public class JitSchema {

  public static void main(String[] args) throws IOException {
    OpenAPI openAPI =
        new OpenAPI()
            .info(
                new Info()
                    .title("DCSA Just in Time Port Calls API")
                    .description("# DCSA OpenAPI specification for Just in Time Port Call process")
                    .version("2.0.0")
                    .license(new License().name("Apache 2.0").url("https://apache.org"))
                    .contact(DCSABase.getDefaultContact()))
            .addTagsItem(
                new io.swagger.v3.oas.models.tags.Tag()
                    .name("Port Call Service - Consumer")
                    .description("**Consumer** implemented endPoints"))
            .addTagsItem(
                new io.swagger.v3.oas.models.tags.Tag()
                    .name("Port Call Service - Provider")
                    .description("**Provider** implemented endPoints"))
            .addTagsItem(
                new io.swagger.v3.oas.models.tags.Tag()
                    .name("Port Call Service")
                    .description("**Provider** and **Consumer** implemented endPoints"))
            .servers(List.of(new Server().url("https://api.example.com")));

    // Extract and register Java class schemas: add Parameters, Headers, and Schemas.
    Components components = new Components();
    ModelConverters.getInstance().addConverter(new ModelValidatorConverter());
    Stream.of(
            PortCall.class,
            TerminalCall.class,
            PortCallService.class,
            Vessel.class,
            ErrorResponse.class,
            DetailedError.class,
            Container.class)
        .forEach(
            modelClass ->
                ModelConverters.getInstance().read(modelClass).forEach(components::addSchemas));
    components.addParameters("Api-Version-Major", SchemaParams.getApiVersionMajorHeader());
    components.addParameters("portCallIDPathParam", SchemaParams.getPortCallIDPathParam());
    components.addParameters(
        "portCallServiceIDPathParam", SchemaParams.getPortCallServiceIDPathParam());
    components.addHeaders(
        "API-Version",
        new Header()
            .description(
                "SemVer used to indicate the version of the contract (API version) returned.")
            .schema(new Schema<>().type("string").example("2.0.0")));
    openAPI.setComponents(components);

    PortCallEndPoint.addEndPoint(openAPI);
    TerminalCallEndPoint.addEndPoint(openAPI);
    PortCallServicesEndPoint.addEndPoint(openAPI);

    // Export to YAML
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

    String yamlFilePath = "jit/src/main/resources/standards/jit/schemas/exported-JIT_v2.0.0.yaml";
    String yamlContent = mapper.writeValueAsString(openAPI);
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    System.out.printf("OpenAPI spec exported to %s%n", yamlFilePath);
  }

  public static ApiResponse getErrorApiResponse() {
    return new ApiResponse()
        .description(
            "In case a server error occurs in implementer system, a `500` (Internal Server Error) is returned.")
        .headers(DCSABase.API_VERSION_HEADER)
        .content(
            new Content()
                .addMediaType(
                    DCSABase.JSON_CONTENT_TYPE,
                    new MediaType().schema(DCSABase.getErrorResponseSchema())));
  }
}
