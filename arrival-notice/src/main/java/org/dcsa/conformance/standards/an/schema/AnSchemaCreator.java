package org.dcsa.conformance.standards.an.schema;

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
import io.swagger.v3.oas.models.media.Schema;
import org.dcsa.conformance.standards.an.schema.model.Address;
import org.dcsa.conformance.standards.an.schema.model.ArrivalNotice;
import org.dcsa.conformance.standards.an.schema.model.Contact;
import org.dcsa.conformance.standards.an.schema.model.DocumentParties;
import org.dcsa.conformance.standards.an.schema.model.DocumentParty;
import org.dcsa.conformance.standards.an.schema.model.IdentifyingPartyCode;
import org.dcsa.conformance.standards.an.schema.model.Location;
import org.dcsa.conformance.standards.an.schema.model.Reference;
import org.dcsa.conformance.standards.an.schema.model.TaxOrLegalReference;
import org.dcsa.conformance.standards.an.schema.model.Transport;
import org.dcsa.conformance.standards.an.schema.model.VesselVoyage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class AnSchemaCreator {

  public static void main(String[] args) throws IOException {
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
                            .email("info@dcsa.org")));

    // Extract and register Java class schemas: add Parameters, Headers, and Schemas.
    Components components = new Components();
    ModelConverters.getInstance().addConverter(new ModelValidatorConverter());
    Stream.of(
            Address.class,
            ArrivalNotice.class,
            Contact.class,
            DocumentParty.class,
            DocumentParties.class,
            IdentifyingPartyCode.class,
            Location.class,
            Reference.class,
            TaxOrLegalReference.class,
            Transport.class,
            VesselVoyage.class)
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

    GetArrivalNoticesEndpoint.addEndPoint(openAPI);

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
        "arrival-notice/src/main/resources/standards/an/schemas/exported-AN_v1.0.0.yaml";
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    System.out.printf("OpenAPI spec exported to %s%n", yamlFilePath);
  }

  static String readResourceFile(String fileName) {
    try {
      return Files.readString(
          Paths.get(
              Objects.requireNonNull(AnSchemaCreator.class.getClassLoader().getResource(fileName))
                  .toURI()));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Failed to read file from resources: " + fileName, e);
    }
  }
}
