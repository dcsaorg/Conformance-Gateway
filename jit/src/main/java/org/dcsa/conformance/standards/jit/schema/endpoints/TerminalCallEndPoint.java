package org.dcsa.conformance.standards.jit.schema.endpoints;

import static org.dcsa.conformance.standards.jit.schema.DCSABase.API_VERSION_HEADER;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.DCSABase;
import org.dcsa.conformance.standards.jit.schema.JitSchema;
import org.dcsa.conformance.standards.jit.schema.SchemaParams;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TerminalCallEndPoint {

  public static void addPortCallEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "/terminal-calls/{terminalCallID}",
        new PathItem()
            .put(
                new Operation()
                    .summary("Initiates a new or updates a Terminal Call")
                    .description(
                        """
Creates or updates a **Terminal Call** record. The caller must provide a unique `terminalCallID` (UUIDv4), which identifies the **Terminal Call**. The `terminalCallID` must remain consistent across all subsequent communications and linked **Port Call Services**. If updating an existing **Terminal Call**, e.g. including the `terminalCallReference`, the provided `terminalCallID` must match the existing record.

The **Terminal Call** includes:
  - link to the **Port Call** (required): `portCallID`
  - Service information (required): `carrierServiceCode`, `carrierServiceName` (and an optional `universalServiceReference`)
  - Voyage information: `carrierImportVoyageNumber`, `carrierExportVoyageNumber`, `universalImportVoyageReference` and `universalExportVoyageReference`
  - terminal information: `terminalCallReference` and `terminalCallSequenceNumber`
  - The ability to send the record with informational purpose only, using `isFYI=true`

This call is often provided as the second call from a **Carrier** to a **Terminal** after the creation of the **Port Call** and then sending `ETA-Berth` or `Moves`.

It is not possible to update a **Terminal Call** that has been `OMITTED`.
""")
                    .operationId("put-terminal-call")
                    .parameters(
                        List.of(
                            new Parameter().$ref(SchemaParams.PORT_CALL_ID_REF),
                            new Parameter().$ref(SchemaParams.API_VERSION_MAJOR_REF)))
                    .tags(Collections.singletonList("Port Call Service - Consumer"))
                    .requestBody(
                        new RequestBody()
                            .description("Initiates a new or updates a Terminal Call")
                            .required(true)
                            .content(
                                new Content()
                                    .addMediaType(
                                        DCSABase.JSON_CONTENT_TYPE,
                                        new MediaType()
                                            .schema(
                                                new Schema<>()
                                                    .$ref("#/components/schemas/TerminalCall")))))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "204",
                                new ApiResponse()
                                    .description(
                                        "A new or updated Terminal Call successfully accepted by consumer.")
                                    .headers(API_VERSION_HEADER))
                            .addApiResponse(
                                "400",
                                new ApiResponse()
                                    .description(
                                        "In case creating a new **Terminal Call** fails schema validation, a `400` (Bad Request) is returned.")
                                    .headers(API_VERSION_HEADER)
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(DCSABase.getErrorResponseSchema()))))
                            .addApiResponse(
                                "409",
                                new ApiResponse()
                                    .description(
                                        "In case creating a new or updating a **Terminal Call** linked to a **Port Call** that has been `OMITTED`, a `409` (Conflict) is returned.")
                                    .headers(API_VERSION_HEADER)
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(DCSABase.getErrorResponseSchema()))))
                            .addApiResponse("500", JitSchema.getErrorApiResponse()))));
  }
}
