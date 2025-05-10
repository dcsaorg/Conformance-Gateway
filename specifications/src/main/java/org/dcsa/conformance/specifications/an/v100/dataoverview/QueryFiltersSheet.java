package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryFiltersSheet extends DataOverviewSheet {
  protected QueryFiltersSheet(Map<Boolean, List<List<Parameter>>> requiredAndOptionalFilters) {
    super(
        "Query filters",
        "QueryFiltersTable",
        1,
        List.of("FilterParameters", "Required"),
        List.of(128, 16),
        List.of(true, false),
        Stream.of(Boolean.TRUE, Boolean.FALSE)
            .flatMap(
                required ->
                    requiredAndOptionalFilters.get(required).stream()
                        .map(
                            parameterList ->
                                List.of(
                                    parameterList.stream()
                                        .map(Parameter::getName)
                                        .collect(Collectors.joining(", ")),
                                    required ? "yes" : "")))
            .toList(),
        importFromCsvFile(
            "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/bd25914e5526aa7c8a67aba8a28010555c82d1ad/specifications/generated-resources/an-v1.0.0-data-overview-query-filters.csv"),
        Map.ofEntries());
  }
}
