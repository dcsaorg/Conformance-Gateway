package org.dcsa.conformance.specifications.standards.vgm.v100;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;

public class GetVGMDeclarationsEndpoint implements QueryParametersFilterEndpoint {

  private final Parameter carrierBookingReference =
      createStringQueryParameter(
          "carrierBookingReference",
          "ABC709951",
          "Reference of the booking for which to return the associated VGMs");

  private final Parameter transportDocumentReference =
      createStringQueryParameter(
          "transportDocumentReference",
          "HHL71800000",
          "Reference of the transport document for which to return the associated VGMs");

  private final Parameter equipmentReference =
      createStringQueryParameter(
          "equipmentReference",
          "APZU4812090",
          "Reference of the equipment for which to return the associated VGMs");

  private final Parameter declarationDateTimeMin =
      createDateTimeQueryParameter(
          "declarationDateTimeMin",
          "Retrieve VGMs with a `declarationDateTime` at or after this timestamp");

  private final Parameter declarationDateTimeMax =
      createDateTimeQueryParameter(
          "declarationDateTimeMax",
          "Retrieve VGMs with a `declarationDateTime` at or before this timestamp");

  private final Parameter limit =
      createIntegerQueryParameter(
          "limit", 10, "Maximum number of VGMs to include in each page of the response.");

  private final Parameter cursor =
      createStringQueryParameter(
          "cursor",
          "ExampleNextPageCursor",
          "Set to the value of the `Next-Page-Cursor` header of the previous response to retrieve the next page.");

  @Override
  public List<Parameter> getQueryParameters() {
    return List.of(
        carrierBookingReference,
        transportDocumentReference,
        equipmentReference,
        declarationDateTimeMin,
        declarationDateTimeMax,
        limit,
        cursor);
  }

  @Override
  public Map<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters() {
    return Map.ofEntries(
        Map.entry(
            Boolean.TRUE,
            allCombinationsOf(
                List.of(
                    List.of(carrierBookingReference),
                    List.of(carrierBookingReference, equipmentReference),
                    List.of(transportDocumentReference),
                    List.of(transportDocumentReference, equipmentReference)),
                List.of(
                    List.of(declarationDateTimeMin),
                    List.of(declarationDateTimeMax),
                    List.of(declarationDateTimeMin, declarationDateTimeMax)))),
        Map.entry(Boolean.FALSE, List.of()));
  }

  private static List<List<Parameter>> allCombinationsOf(
      List<List<Parameter>> leftListList, List<List<Parameter>> rightListList) {
    return leftListList.stream()
        .flatMap(
            leftList ->
                rightListList.stream()
                    .map(
                        rightList -> Stream.concat(leftList.stream(), rightList.stream()).toList()))
        .toList();
  }

  private static Parameter createStringQueryParameter(
      String name, String example, String description) {
    return new Parameter()
        .in("query")
        .name(name)
        .example(example)
        .description(description)
        .schema(new Schema<String>().type("string"));
  }

  private Parameter createDateTimeQueryParameter(String name, String description) {
    return new Parameter()
        .in("query")
        .name(name)
        .description(description)
        .example("2025-01-23T01:23:45Z")
        .schema(new Schema<String>().type("string").format("date-time"));
  }

  private Parameter createIntegerQueryParameter(String name, int example, String description) {
    return new Parameter()
        .in("query")
        .name(name)
        .description(description)
        .example(example)
        .schema(new Schema<Integer>().type("integer").format("int32"));
  }
}
