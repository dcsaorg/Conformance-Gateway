package org.dcsa.conformance.specifications.standards.an.v100;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;

public class GetArrivalNoticesEndpoint implements QueryParametersFilterEndpoint {

  private final Parameter transportDocumentReferences =
      new Parameter()
          .in("query")
          .name("transportDocumentReferences")
          .schema(new ArraySchema().items(new StringSchema()))
          .explode(Boolean.FALSE)
          .description(
              "Reference(s) of the transport document(s) for which to return the associated arrival notices")
          .example("TDR0123456,TDR1234567");

  private final Parameter equipmentReferences =
      new Parameter()
          .in("query")
          .name("equipmentReferences")
          .schema(new ArraySchema().items(new StringSchema()))
          .explode(Boolean.FALSE)
          .description(
              "Reference(s) of the equipment for which to return the associated arrival notices")
          .example("APZU4812090,APZU4812091");

  private final Parameter portOfDischarge =
      new Parameter()
          .in("query")
          .name("portOfDischarge")
          .description(
              "UN location of the port of discharge for which to retrieve available arrival notices")
          .example("NLRTM")
          .schema(new Schema<String>().type("string"));

  private final Parameter vesselIMONumber =
      new Parameter()
          .in("query")
          .name("vesselIMONumber")
          .description("IMO number of the vessel for which to retrieve available arrival notices")
          .example("12345678")
          .schema(new Schema<String>().type("string"));

  private final Parameter vesselName =
      new Parameter()
          .in("query")
          .name("vesselName")
          .description("Name of the vessel for which to retrieve available arrival notices")
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

  private final Parameter minEtaAtPortOfDischargeDate =
      new Parameter()
          .in("query")
          .name("minEtaAtPortOfDischargeDate")
          .description(
              "Retrieve arrival notices with an ETA at port of discharge on or after this date")
          .example("2025-01-23")
          .schema(new Schema<String>().type("string").format("date"));

  private final Parameter maxEtaAtPortOfDischargeDate =
      new Parameter()
          .in("query")
          .name("maxEtaAtPortOfDischargeDate")
          .description(
              "Retrieve arrival notices with an ETA at port of discharge on or before this date")
          .example("2025-01-23")
          .schema(new Schema<String>().type("string").format("date"));

  private final Parameter removeCharges =
      new Parameter()
          .in("query")
          .name("removeCharges")
          .description(
"""
Optional flag indicating whether the publisher should remove the charges from the PDF visualization
of every returned arrival notice, and for consistency, also from the structured response data.

This flag allows arrival notice receivers to retrieve, for the purpose of forwarding to third parties,
versions of the arrival notice PDF visualizations in which the charges are removed,
if they would be normally received with charges included based on the role of the arrival notice recipient.

This flag is **not** expected to perform any filtering on the list of arrival notices included in the response.
However, if the removal of charges (from the arrival notices that have them) results in a list of arrival notices
in which some become exact duplicates, publishers may choose to remove duplicates from the response.

The default value is `false`, which leaves unchanged the presence or absence of charges in each returned arrival notice.
""")
          .example(true)
          .schema(new Schema<Boolean>().type("boolean"));

  private final Parameter includeVisualization =
      new Parameter()
          .in("query")
          .name("includeVisualization")
          .description(
"""
Optional flag indicating whether the PDF `arrivalNoticeVisualization` should be included in each returned arrival notice.

The publisher makes the final decision on whether to include PDF visualizations in the response (for some or for all
the arrival notices), based on a variety of factors including:
- whether it has implemented support for including PDF visualizations
- the API consumer (role, registration profile, business relationship)
- the type and availability status of the returned arrival notices.

However, to support a future transition to fully automated processing of arrival notices by receivers,
the publisher should **not** include the PDF visualization if this parameter is set to `false`.
""")
          .example(true)
          .schema(new Schema<Boolean>().type("boolean"));

  private final Parameter limit =
      new Parameter()
          .in("query")
          .name("limit")
          .description("Maximum number of arrival notices to include in each page of the response.")
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
        portOfDischarge,
        vesselIMONumber,
        vesselName,
        carrierImportVoyageNumber,
        universalImportVoyageReference,
        carrierServiceCode,
        universalServiceReference,
        minEtaAtPortOfDischargeDate,
        maxEtaAtPortOfDischargeDate,
        includeVisualization,
        removeCharges,
        limit,
        cursor);
  }

  @Override
  public Map<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters() {
    return Map.ofEntries(
        Map.entry(
            Boolean.TRUE,
            List.of(
                // TDR only
                List.of(transportDocumentReferences))),
        Map.entry(
            Boolean.FALSE,
            List.of(
                // EQR
                List.of(transportDocumentReferences, equipmentReferences),
                List.of(equipmentReferences),
                List.of(
                    minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate, equipmentReferences),
                // POD
                List.of(minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate, portOfDischarge),
                // vessel IMO + voyage number (+ service)
                List.of(minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate, vesselIMONumber),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    carrierImportVoyageNumber),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    carrierImportVoyageNumber,
                    carrierServiceCode),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    carrierImportVoyageNumber,
                    universalServiceReference),
                // vessel IMO + voyage reference (+ service)
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    universalImportVoyageReference),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    universalImportVoyageReference,
                    carrierServiceCode),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselIMONumber,
                    universalImportVoyageReference,
                    universalServiceReference),
                // vessel name + voyage number (+ service)
                List.of(minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate, vesselName),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    carrierImportVoyageNumber),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    carrierImportVoyageNumber,
                    carrierServiceCode),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    carrierImportVoyageNumber,
                    universalServiceReference),
                // vessel name + voyage reference (+ service)
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    universalImportVoyageReference),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    universalImportVoyageReference,
                    carrierServiceCode),
                List.of(
                    minEtaAtPortOfDischargeDate,
                    maxEtaAtPortOfDischargeDate,
                    vesselName,
                    universalImportVoyageReference,
                    universalServiceReference))));
  }
}
