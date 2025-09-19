package org.dcsa.conformance.specifications.standards.tnt.v300;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;

public class GetEventsEndpoint implements QueryParametersFilterEndpoint {

  // eventRouting.originatingParty

  private final Parameter transportDocumentReferences =
      createStringArrayParameter(
          "transportDocumentReferences",
          "Reference(s) of the transport document(s) for which to return the associated events",
          "TDR0123456,TDR1234567");

  private final Parameter equipmentReferences =
      createStringArrayParameter(
          "equipmentReferences",
          "Reference(s) of the equipment for which to return the associated events",
          "APZU4812090,APZU4812091");

  private final Parameter vesselIMONumber =
      createStringParameter(
          "vesselIMONumber",
          "IMO number of the vessel for which to retrieve available events",
          "12345678");

  private final Parameter vesselName =
      createStringParameter(
          "vesselName",
          "Name of the vessel for which to retrieve available events",
          "King of the Seas");

  private final Parameter carrierImportVoyageNumber =
      createStringParameter(
          "carrierImportVoyageNumber",
          "The identifier of an import voyage. The carrier-specific identifier of the import Voyage.",
          "1234N");

  private final Parameter universalImportVoyageReference =
      createStringParameter(
          "universalImportVoyageReference",
          "A global unique voyage reference for the import Voyage, as per DCSA standard, agreed by VSA partners for the voyage.",
          "2301W");

  private final Parameter carrierExportVoyageNumber =
      createStringParameter(
          "carrierExportVoyageNumber",
          "The identifier of an export voyage. The carrier-specific identifier of the export Voyage.",
          "1234N");

  private final Parameter universalExportVoyageReference =
      createStringParameter(
          "universalExportVoyageReference",
          "A global unique voyage reference for the export Voyage, as per DCSA standard, agreed by VSA partners for the voyage.",
          "2301W");

  private final Parameter carrierServiceCode =
      createStringParameter(
          "carrierServiceCode",
          "The carrier specific code of the service for which the schedule details are published.",
          "FE1");

  private final Parameter universalServiceReference =
      createStringParameter(
          "universalServiceReference",
          "A global unique service reference, as per DCSA standard, agreed by VSA partners for the service.",
          "SR12345A");

  private final Parameter dateTimeMin =
      createDateTimeParameter(
          "dateTimeMin",
          "Retrieve events with an `eventUpdatedDateTime` at or after this timestamp");

  private final Parameter dateTimeMax =
      createDateTimeParameter(
          "dateTimeMax",
          "Retrieve events with an `eventUpdatedDateTime` at or before this timestamp");

  private final Parameter limit =
      new Parameter()
          .in("query")
          .name("limit")
          .description("Maximum number of events to include in each page of the response.")
          .example(10)
          .schema(new Schema<Integer>().type("integer").format("int32").minimum(new BigDecimal(1)));

  private final Parameter cursor =
      createStringParameter(
          "cursor",
          "Set to the value of the `Next-Page-Cursor` header of the previous response to retrieve the next page.",
          "ExampleNextPageCursor");

  @Override
  public List<Parameter> getQueryParameters() {
    return List.of(
        transportDocumentReferences,
        equipmentReferences,
        vesselIMONumber,
        vesselName,
        carrierExportVoyageNumber,
        universalExportVoyageReference,
        carrierImportVoyageNumber,
        universalImportVoyageReference,
        carrierServiceCode,
        universalServiceReference,
        dateTimeMin,
        dateTimeMax,
        limit,
        cursor);
  }

  @Override
  public Map<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters() {
    return Map.ofEntries(
        Map.entry(
            Boolean.TRUE,
            List.of(List.of(transportDocumentReferences), List.of(equipmentReferences))),
        Map.entry(
            Boolean.FALSE, List.of(List.of(transportDocumentReferences, equipmentReferences))));
  }

  private Parameter createDateTimeParameter(String name, String description) {
    return new Parameter()
        .in("query")
        .name(name)
        .description(description)
        .example("2025-01-23T01:23:45Z")
        .schema(new Schema<String>().type("string").format("date-time"));
  }

  private Parameter createStringParameter(String name, String description, String example) {
    return new Parameter()
        .in("query")
        .name(name)
        .description(description)
        .example(example)
        .schema(new Schema<String>().type("string"));
  }

  private Parameter createStringArrayParameter(String name, String description, String example) {
    return new Parameter()
        .in("query")
        .name(name)
        .schema(new ArraySchema().items(new StringSchema()))
        .explode(Boolean.FALSE)
        .description(description)
        .example(example);
  }
}
