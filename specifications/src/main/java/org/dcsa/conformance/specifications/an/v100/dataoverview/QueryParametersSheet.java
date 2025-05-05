package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;

public class QueryParametersSheet extends DataOverviewSheet {
  protected QueryParametersSheet(List<Parameter> queryParameters) {
    super(
        "Query parameters",
        "QueryParametersTable",
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
            .toList());
  }
}
