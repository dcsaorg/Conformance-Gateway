package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import java.io.IOException;
import java.util.stream.Stream;
import org.dcsa.conformance.standards.jit.schema.common.DCSABase;
import org.dcsa.conformance.standards.jit.schema.common.ModelValidatorConverter;
import org.dcsa.conformance.standards.jit.schema.endpoints.GetPortCallsEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.PortCallEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.PortCallOmitEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.PortCallServicesEndPoint;
import org.dcsa.conformance.standards.jit.schema.endpoints.TerminalCallEndPoint;
import org.dcsa.conformance.standards.jit.schema.model.DetailedError;
import org.dcsa.conformance.standards.jit.schema.model.ErrorResponse;
import org.dcsa.conformance.standards.jit.schema.model.OmitPortCall;
import org.dcsa.conformance.standards.jit.schema.model.OmitTerminalCall;
import org.dcsa.conformance.standards.jit.schema.model.PortCall;
import org.dcsa.conformance.standards.jit.schema.model.PortCallService;
import org.dcsa.conformance.standards.jit.schema.model.TerminalCall;
import org.dcsa.conformance.standards.jit.schema.model.Vessel;

public class JitSchemaCreator {

  public static void main(String[] args) throws IOException {
    OpenAPI openAPI =
        new OpenAPI()
            .openapi("3.0.3")
            .info(
                new Info()
                    .version("2.0.0")
                    .title("DCSA Just in Time Port Calls API")
                    .description(
                        DCSABase.readFileFromResources(
                            "standards/jit/schemas/standard-full-description.md"))
                    .license(DCSABase.getDefaultLicense())
                    .contact(DCSABase.getDefaultContact()))
            .addTagsItem(
                new Tag()
                    .name("Port Call Service - Service Consumer")
                    .description("**Service Consumer** implemented endPoints"))
            .addTagsItem(
                new Tag()
                    .name("Port Call Service - Service Provider")
                    .description("**Service Provider** implemented endPoints"))
            .addTagsItem(
                new Tag()
                    .name("Port Call Service")
                    .description(
                        "**Service Provider** and **Service Consumer** implemented endPoints"));

    // Extract and register Java class schemas: add Parameters, Headers, and Schemas.
    Components components = new Components();
    ModelConverters.getInstance().addConverter(new ModelValidatorConverter());
    Stream.of(
            PortCall.class,
            TerminalCall.class,
            PortCallService.class,
            Vessel.class,
            OmitPortCall.class,
            OmitTerminalCall.class,
            ErrorResponse.class,
            DetailedError.class)
        .forEach(
            modelClass ->
                ModelConverters.getInstance().read(modelClass).forEach(components::addSchemas));
    components.addSchemas(
        "PortCalls",
        new ArraySchema()
            .description("An array of **Port Call** objects matching the filters provided.")
            .items(new Schema<>().$ref("#/components/schemas/PortCall"))
            .minItems(0));

    components.addParameters("Api-Version-Major", JitSchemaComponents.getApiVersionMajorHeader());
    components.addParameters("portCallIDPathParam", JitSchemaComponents.getPortCallIDPathParam());
    components.addParameters(
        "portCallServiceIDPathParam", JitSchemaComponents.getPortCallServiceIDPathParam());

    JitSchemaComponents.addHeaders(components);
    openAPI.setComponents(components);

    PortCallEndPoint.addEndPoint(openAPI);
    PortCallOmitEndPoint.addEndPoint(openAPI);
    GetPortCallsEndPoint.addEndPoint(openAPI);
    TerminalCallEndPoint.addEndPoint(openAPI);
    PortCallServicesEndPoint.addEndPoint(openAPI);

    DCSABase.generateYamlFile(
        openAPI, "jit/src/main/resources/standards/jit/schemas/exported-JIT_v2.0.0.yaml");
  }

  public static ApiResponse getErrorApiResponse() {
    return new ApiResponse()
        .description(
            "In case a server error occurs in implementer system, a `500` (Internal Server Error) is returned.")
        .headers(JitSchemaComponents.getDefaultJitHeaders())
        .content(
            new Content()
                .addMediaType(
                    DCSABase.JSON_CONTENT_TYPE,
                    new MediaType().schema(DCSABase.getErrorResponseSchema())));
  }
}
