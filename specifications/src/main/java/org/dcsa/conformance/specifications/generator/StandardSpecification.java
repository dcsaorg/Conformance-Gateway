package org.dcsa.conformance.specifications.generator;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.dataoverview.DataOverview;
import org.dcsa.conformance.specifications.dataoverview.DataOverviewSheet;

@Slf4j
public abstract class StandardSpecification {
  private final String standardAbbreviation;
  private final String standardVersion;

  protected final OpenAPI openAPI;

  private final Map<String, Map<String, List<SchemaConstraint>>> constraintsByClassAndField;

  protected StandardSpecification(
      String standardName,
      String standardAbbreviation,
      String standardVersion) {
    this.standardAbbreviation = standardAbbreviation;
    this.standardVersion = standardVersion;
    openAPI =
        new OpenAPI()
            .openapi("3.0.3")
            .info(
                new Info()
                    .version(standardVersion)
                    .title("DCSA %s API".formatted(standardName))
                    .description(
                        SpecificationToolkit.readResourceFile(
                            "conformance/specifications/%s/v%s/openapi-root.md"
                                .formatted(
                                    standardAbbreviation.toLowerCase(),
                                    standardVersion.replaceAll("\\.", ""))))
                    .license(
                        new License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                    .contact(
                        new io.swagger.v3.oas.models.info.Contact()
                            .name("Digital Container Shipping Association (DCSA)")
                            .url("https://dcsa.org")
                            .email("info@dcsa.org")))
            .components(new Components());

    openAPI
        .getComponents()
        .addHeaders(
            "API-Version",
            new Header()
                .description(
                    "SemVer used to indicate the version of the contract (API version) returned.")
                .schema(new Schema<>().type("string").example(standardVersion)));

    constraintsByClassAndField = new HashMap<>();
    modelClassesStream()
        .forEach(
            modelClass ->
                SpecificationToolkit.getClassConstraints(modelClass.getName())
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

    ModelConverters.getInstance()
        .addConverter(new ModelValidatorConverter(constraintsByClassAndField));
    modelClassesStream()
        .forEach(
            modelClass ->
                ModelConverters.getInstance()
                    .read(modelClass)
                    .forEach(openAPI.getComponents()::addSchemas));
  }

  protected abstract Stream<Class<?>> modelClassesStream();

  protected abstract List<String> getRootTypeNames();

  protected abstract Map<Class<? extends DataOverviewSheet>, List<List<String>>>
      getOldDataValuesBySheetClass();

  protected abstract Map<Class<? extends DataOverviewSheet>, Map<String, String>>
      getChangedPrimaryKeyByOldPrimaryKeyBySheetClass();

  protected abstract QueryParametersFilterEndpoint getQueryParametersFilterEndpoint();

  @SneakyThrows
  public void generateArtifacts() {
    String yamlContent = SpecificationToolkit.createYamlObjectMapper().writeValueAsString(openAPI);
    String lowerCaseStandardAbbreviation = standardAbbreviation.toLowerCase();
    String fileNamePrefix = "%s-v%s".formatted(lowerCaseStandardAbbreviation, standardVersion);
    String exportFileDir =
        "./generated-resources/standards/%s/v%s/"
            .formatted(lowerCaseStandardAbbreviation, standardVersion.replaceAll("\\.", ""));
    String yamlFilePath = exportFileDir + "%s-openapi.yaml".formatted(fileNamePrefix);
    Files.writeString(Paths.get(yamlFilePath), yamlContent);
    log.info("OpenAPI spec exported to {}", yamlFilePath);

    DataOverview dataOverview =
        new DataOverview(
            constraintsByClassAndField,
            SpecificationToolkit.parameterizeStringRawSchemaMap(
                openAPI.getComponents().getSchemas()),
            getRootTypeNames(),
            getQueryParametersFilterEndpoint().getQueryParameters(),
            getQueryParametersFilterEndpoint().getRequiredAndOptionalFilters(),
            getOldDataValuesBySheetClass(),
            getChangedPrimaryKeyByOldPrimaryKeyBySheetClass());
    dataOverview.exportToExcelFile(
        exportFileDir + "%s-data-overview.xlsx".formatted(fileNamePrefix));
    dataOverview.exportToCsvFiles(
        exportFileDir + "%s-data-overview-%s.csv".formatted(fileNamePrefix, "%s")); // pass the %s
  }
}
