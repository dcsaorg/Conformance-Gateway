package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;

public class QueryParametersSheet extends DataOverviewSheet {
  protected QueryParametersSheet(List<Parameter> queryParameters) {
    super(
        "Query parameters",
        "QueryParametersTable",
        1,
        List.of("Name", "Type", "Description", "Example"),
        List.of(32, 16, 120, 32),
        List.of(false, false, true, false),
        queryParameters.stream()
            .map(
                queryParameter ->
                    List.of(
                        queryParameter.getName(),
                        queryParameter.getSchema().getType(),
                        queryParameter.getDescription().trim(),
                        String.valueOf(queryParameter.getExample()).trim()))
            .toList(),
        importFromCsvFile(
            "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/bd25914e5526aa7c8a67aba8a28010555c82d1ad/specifications/generated-resources/an-v1.0.0-data-overview-query-parameters.csv"),
        Map.ofEntries());
  }
}
