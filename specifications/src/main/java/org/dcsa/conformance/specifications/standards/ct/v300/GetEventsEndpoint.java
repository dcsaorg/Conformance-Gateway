package org.dcsa.conformance.specifications.standards.ct.v300;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;

public class GetEventsEndpoint implements QueryParametersFilterEndpoint {

  private final Parameter transportDocumentReferences =
      new Parameter()
          .in("query")
          .name("transportDocumentReferences")
          .schema(new ArraySchema().items(new StringSchema()))
          .explode(Boolean.FALSE)
          .description(
              "Reference(s) of the transport document(s) for which to return the associated events")
          .example("TDR0123456,TDR1234567");

  private final Parameter equipmentReferences =
      new Parameter()
          .in("query")
          .name("equipmentReferences")
          .schema(new ArraySchema().items(new StringSchema()))
          .explode(Boolean.FALSE)
          .description("Reference(s) of the equipment for which to return the associated events")
          .example("APZU4812090,APZU4812091");

  private final Parameter vesselIMONumber =
      new Parameter()
          .in("query")
          .name("vesselIMONumber")
          .description("IMO number of the vessel for which to retrieve available events")
          .example("12345678")
          .schema(new Schema<String>().type("string"));

  private final Parameter vesselName =
      new Parameter()
          .in("query")
          .name("vesselName")
          .description("Name of the vessel for which to retrieve available events")
          .example("King of the Seas")
          .schema(new Schema<String>().type("string"));

  private final Parameter carrierImportVoyageNumber =
      new Parameter()
          .in("query")
          .name("carrierImportVoyageNumber")
          .description(
              "The identifier of an import voyage. The carrier-specific identifier of the import Voyage.")
          .example("1234N")
          .schema(new Schema<String>().type("string"));

  private final Parameter universalImportVoyageReference =
      new Parameter()
          .in("query")
          .name("universalImportVoyageReference")
          .description(
              "A global unique voyage reference for the import Voyage, as per DCSA standard, agreed by VSA partners for the voyage.")
          .example("2301W")
          .schema(new Schema<String>().type("string"));

  private final Parameter carrierExportVoyageNumber =
      new Parameter()
          .in("query")
          .name("carrierExportVoyageNumber")
          .description(
              "The identifier of an export voyage. The carrier-specific identifier of the export Voyage.")
          .example("1234N")
          .schema(new Schema<String>().type("string"));

  private final Parameter universalExportVoyageReference =
      new Parameter()
          .in("query")
          .name("universalExportVoyageReference")
          .description(
              "A global unique voyage reference for the export Voyage, as per DCSA standard, agreed by VSA partners for the voyage.")
          .example("2301W")
          .schema(new Schema<String>().type("string"));

  private final Parameter carrierServiceCode =
      new Parameter()
          .in("query")
          .name("carrierServiceCode")
          .description(
              "The carrier specific code of the service for which the schedule details are published.")
          .example("FE1")
          .schema(new Schema<String>().type("string"));

  private final Parameter universalServiceReference =
      new Parameter()
          .in("query")
          .name("universalServiceReference")
          .description(
              "A global unique service reference, as per DCSA standard, agreed by VSA partners for the service.")
          .example("SR12345A")
          .schema(new Schema<String>().type("string"));

  private final Parameter eventDateTimeMin =
      new Parameter()
          .in("query")
          .name("eventDateTimeMin")
          .description("Retrieve events occurring at or after this timestamp")
          .example("2025-01-23T01:23:45Z")
          .schema(new Schema<String>().type("string").format("datetime"));

  private final Parameter eventDateTimeMax =
      new Parameter()
          .in("query")
          .name("eventDateTimeMax")
          .description("Retrieve events occurring at or before this timestamp")
          .example("2025-01-23T01:23:45Z")
          .schema(new Schema<String>().type("string").format("datetime"));

  private final Parameter limit =
      new Parameter()
          .in("query")
          .name("limit")
          .description("Maximum number of events to include in each page of the response.")
          .example(10)
          .schema(new Schema<Integer>().type("number").format("int32").minimum(new BigDecimal(1)));

  private final Parameter cursor =
      new Parameter()
          .in("query")
          .name("cursor")
          .description(
              "Set to the value of the `Next-Page-Cursor` header of the previous response to retrieve the next page.")
          .example("ExampleNextPageCursor")
          .schema(new Schema<String>().type("string"));

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
        eventDateTimeMin,
        eventDateTimeMax,
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
}
