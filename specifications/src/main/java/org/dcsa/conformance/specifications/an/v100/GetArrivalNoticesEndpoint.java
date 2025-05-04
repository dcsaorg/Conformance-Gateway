package org.dcsa.conformance.specifications.an.v100;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.LinkedHashMap;
import java.util.List;

public class GetArrivalNoticesEndpoint {

  private final Parameter transportDocumentReference =
      new Parameter()
          .in("query")
          .name("transportDocumentReference")
          .description(
              "Reference of the transport document for which to return the associated arrival notices")
          .example("TDR0123456")
          .schema(new Schema<String>().type("string"));

  private final Parameter equipmentReference =
      new Parameter()
          .in("query")
          .name("equipmentReference")
          .description(
              "Reference(s) of the equipment for which to return the associated arrival notices")
          .example("APZU4812090,APZU4812091")
          .schema(OpenApiToolkit.stringListQueryParameterSchema());

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

  public List<Parameter> getQueryParameters() {
    return List.of(
        transportDocumentReference,
        equipmentReference,
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

  public LinkedHashMap<Boolean, List<List<Parameter>>> getRequiredAndOptionalFilters() {
    LinkedHashMap<Boolean, List<List<Parameter>>> filters = new LinkedHashMap<>();
    filters.put(
        Boolean.TRUE,
        List.of(
            // TDR only
            List.of(transportDocumentReference)));
    filters.put(
        Boolean.FALSE,
        List.of(
            // TDR + EQR
            List.of(transportDocumentReference, equipmentReference),
            // EQR
            List.of(equipmentReference, minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate),
            // POD
            List.of(portOfDischarge, minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate),
            // vessel
            List.of(vesselIMONumber, minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate),
            List.of(vesselName, minEtaAtPortOfDischargeDate, maxEtaAtPortOfDischargeDate),
            // vessel IMO + voyage
            List.of(
                vesselIMONumber,
                carrierImportVoyageNumber,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselIMONumber,
                universalImportVoyageReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            // vessel name + voyage
            List.of(
                vesselName,
                carrierImportVoyageNumber,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselName,
                universalImportVoyageReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            // vessel IMO + voyage + service
            List.of(
                vesselIMONumber,
                carrierImportVoyageNumber,
                carrierServiceCode,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselIMONumber,
                carrierImportVoyageNumber,
                universalServiceReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselIMONumber,
                universalImportVoyageReference,
                carrierServiceCode,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselIMONumber,
                universalImportVoyageReference,
                universalServiceReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            // vessel name + voyage + service
            List.of(
                vesselName,
                carrierImportVoyageNumber,
                carrierServiceCode,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselName,
                carrierImportVoyageNumber,
                universalServiceReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselName,
                universalImportVoyageReference,
                carrierServiceCode,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate),
            List.of(
                vesselName,
                universalImportVoyageReference,
                universalServiceReference,
                minEtaAtPortOfDischargeDate,
                maxEtaAtPortOfDischargeDate)));
    return filters;
  }
}
