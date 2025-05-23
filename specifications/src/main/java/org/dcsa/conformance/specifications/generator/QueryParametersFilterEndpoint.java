package org.dcsa.conformance.specifications.generator;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;

public interface QueryParametersFilterEndpoint {
  List<Parameter> getQueryParameters();

  Map<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters();
}
