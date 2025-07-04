package org.dcsa.conformance.specifications.standards.an.v100;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
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

  private final Parameter includeCharges =
      new Parameter()
          .in("query")
          .name("includeCharges")
          .description(
"""
Flag indicating whether to include arrival notice charges. If not specified, the default value is `true`.
This flag is separate from the mandatory and optional lists of query parameters that can be used as filters
and can be used in combination with any such filter.
""")
          .example(true)
          .schema(new Schema<Boolean>().type("boolean"));

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
        includeCharges);
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
