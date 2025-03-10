package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.Components;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class JitSchema {

  public static void main(String[] args) throws IOException {
    // Create OpenAPI object
    OpenAPI openAPI =
        new OpenAPI()
            .info(
                new Info()
                    .title("Vessel API")
                    .description(
                        """
                        API to retrieve vessel details and their containers

                        Soon to be replaced with something more useful 🙂
                        """)
                    .version("1.0.0")
                    .license(new License().name("Apache 2.0").url("http://apache.org")))
            .servers(List.of(new Server().url("https://api.example.com")));

    // Extract and register Java class schemas
    Components components = new Components();
    Stream.of(Vessel.class, Container.class)
        .forEach(
            modelClass ->
                ModelConverters.getInstance().read(modelClass).forEach(components::addSchemas));
    openAPI.setComponents(components);

    // Define GET /vessels/{imo} endpoint
    openAPI.path(
        "/vessels/{imo}",
        new PathItem()
            .get(
                new Operation()
                    .summary("Get Vessel by IMO Number")
                    .description(
                        "Retrieves vessel details, including its containers, by IMO number.")
                    .operationId("getVesselByIMO")
                    .parameters(
                        List.of(
                            new Parameter()
                                .name("imo")
                                .in("path")
                                .required(true)
                                .description("IMO number of the vessel")
                                .schema(new Schema<String>().type("string").example("9811000"))))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "200",
                                new ApiResponse()
                                    .description("Successful Response")
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                "application/json",
                                                new MediaType()
                                                    .schema(
                                                        new Schema<>()
                                                            .$ref("#/components/schemas/Vessel")))))
                            .addApiResponse(
                                "404", new ApiResponse().description("Vessel not found")))));

    // Export to YAML
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    objectMapper.findAndRegisterModules();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
    objectMapper.addMixIn(Schema.class, SchemaMixin.class);
    String yamlFilePath = "jit/src/main/resources/standards/jit/schemas/exported-JIT_v2.0.0.yaml";
    String yamlContent = objectMapper.writeValueAsString(openAPI);
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    System.out.printf("OpenAPI spec exported to %s%n", yamlFilePath);
  }
}
