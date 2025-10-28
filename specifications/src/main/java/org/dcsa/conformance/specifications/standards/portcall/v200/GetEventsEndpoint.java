package org.dcsa.conformance.specifications.standards.portcall.v200;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;

public class GetEventsEndpoint implements QueryParametersFilterEndpoint {

  private final Parameter unLocationCode =
      createStringQueryParameter("UNLocationCode", "NLAMS", "UN location code.");

  private final Parameter portVisitReference =
      createStringQueryParameter("portVisitReference", "NLAMS1234589", "Port visit reference.");

  private final Parameter carrierServiceName =
      createStringQueryParameter(
          "carrierServiceName", "Great Lion Service", "Carrier-specific service name.");

  private final Parameter carrierServiceCode =
      createStringQueryParameter("carrierServiceCode", "FE1", "Carrier-specific service code.");

  private final Parameter universalServiceReference =
      createStringQueryParameter(
          "universalServiceReference",
          "SR12345A",
        "Unique identifier of a liner service, defined and distributed by DCSA to carriers.");

  private final Parameter terminalCallReference =
      createStringQueryParameter(
          "terminalCallReference",
          "15063401",
          "The terminal call reference for which to retrieve available events.");

  private final Parameter carrierImportVoyageNumber =
      createStringQueryParameter(
          "carrierImportVoyageNumber",
          "1234N",
          "The identifier of an import voyage. The carrier-specific identifier of the import Voyage.");

  private final Parameter universalImportVoyageReference =
      createStringQueryParameter(
          "universalImportVoyageReference",
          "2301W",
        "Unique identifier of the import voyage within the service, assigned by carriers as specified by DCSA.");

  private final Parameter carrierExportVoyageNumber =
      createStringQueryParameter(
          "carrierExportVoyageNumber",
          "1234N",
          "The identifier of an export voyage. The carrier-specific identifier of the export Voyage.");

  private final Parameter universalExportVoyageReference =
      createStringQueryParameter(
          "universalExportVoyageReference",
          "2301W",
        "Unique identifier of the export voyage within the service, assigned by carriers as specified by DCSA.");

  private final Parameter portCallServiceTypeCode =
      createStringQueryParameter("portCallServiceTypeCode", "BERTH", "Port call service type.");

  private final Parameter vesselIMONumber =
      createStringQueryParameter("vesselIMONumber", "12345678", "Vessel IMO number.");

  private final Parameter vesselName =
      createStringQueryParameter("vesselName", "King of the Seas", "Vessel name.");

  private final Parameter vesselMMSINumber =
      createStringQueryParameter("vesselMMSINumber", "278111222", "Vessel MMSI number.");

  private final Parameter portCallID =
      createStringQueryParameter(
          "portCallID", "0342254a-5927-4856-b9c9-aa12e7c00563", "Unique identifier of a port call");

  private final Parameter terminalCallID =
      createStringQueryParameter(
          "terminalCallID",
          "0342254a-5927-4856-b9c9-aa12e7c00563",
          "Unique identifier of a terminal call");

  private final Parameter portCallServiceID =
      createStringQueryParameter(
          "portCallServiceID",
          "0342254a-5927-4856-b9c9-aa12e7c00563",
          "Unique identifier of a port call service");

  private final Parameter timestampID =
      createStringQueryParameter(
          "timestampID",
          "0342254a-5927-4856-b9c9-aa12e7c00563",
          "Unique identifier of a timestamp");

  private final Parameter classifierCode =
      createStringQueryParameter(
          "classifierCode", "ACT", "Classifier code (EST / REQ / PLN / ACT)");

  private final Parameter eventTimestampMin =
      new Parameter()
          .in("query")
          .name("eventTimestampMin")
          .description("Retrieve events with a timestamp at or after this date-time")
          .example("2025-01-23T01:23:45Z")
          .schema(new Schema<String>().type("string").format("date-time"));

  private final Parameter eventTimestampMax =
      new Parameter()
          .in("query")
          .name("eventTimestampMax")
          .description("Retrieve events with a timestamp at or before this date-time")
          .example("2025-01-23T01:23:45Z")
          .schema(new Schema<String>().type("string").format("date-time"));

  private final Parameter limit =
      new Parameter()
          .in("query")
          .name("limit")
          .description("Maximum number of events to include in each page of the response.")
          .example(10)
          .schema(new Schema<Integer>().type("integer").format("int32").minimum(new BigDecimal(1)));

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
        unLocationCode,
        portVisitReference,
        carrierServiceName,
        carrierServiceCode,
        universalServiceReference,
        terminalCallReference,
        carrierImportVoyageNumber,
        universalImportVoyageReference,
        carrierExportVoyageNumber,
        universalExportVoyageReference,
        portCallServiceTypeCode,
        vesselIMONumber,
        vesselName,
        vesselMMSINumber,
        portCallID,
        terminalCallID,
        portCallServiceID,
        timestampID,
        classifierCode,
      eventTimestampMin,
      eventTimestampMax,
        limit,
        cursor);
  }

  @Override
  public Map<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters() {
    return Map.ofEntries(Map.entry(Boolean.TRUE, List.of()), Map.entry(Boolean.FALSE, List.of()));
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
}
