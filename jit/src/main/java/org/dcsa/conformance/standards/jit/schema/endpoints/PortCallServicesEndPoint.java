package org.dcsa.conformance.standards.jit.schema.endpoints;

import static org.dcsa.conformance.standards.jit.schema.common.DCSABase.API_VERSION_HEADER;

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
import org.dcsa.conformance.standards.jit.schema.JitSchema;
import org.dcsa.conformance.standards.jit.schema.SchemaParams;
import org.dcsa.conformance.standards.jit.schema.common.DCSABase;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PortCallServicesEndPoint {

  public static void addEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "/port-call-services/{portCallServiceID}",
        new PathItem()
            .put(
                new Operation()
                    .summary("Initiates a new or updates a Port Call Service")
                    .description(
                        """
Creates or updates a **Port Call Service** record. The caller must provide a unique `portCallServiceID` (UUIDv4), which identifies the **Port Call Service**. The `portCallServiceID` must remain consistent across all subsequent communications and linked **Timestamps**. If updating an existing **Port Call Service**, e.g. updating the `moves`, the provided `portCallServiceID` must match the existing record.

The **Port Call Service** includes:

 - link to the **Terminal Call** (required): `terminalCallID`
 - type of Service (required): `portCallServiceTypeCode` and `portCallServiceEventTypeCode` (and optionally `portCallPhaseTypeCode` and `facilityTypeCode`)
 - a location (required): `portCallServiceLocation`
 - Moves forecast information: `moves`
 - The ability to send the record with informational purpose only, using `isFYI=true`

This call is used to initiate a Service linked to a **Terminal Call**. It is used for sending e.g. `ETA-Berth` or `Moves`.
""")
                    .operationId("put-port-call-service")
                    .parameters(
                        List.of(
                            new Parameter().$ref(SchemaParams.PORT_CALL_SERVICE_ID_REF),
                            new Parameter().$ref(SchemaParams.API_VERSION_MAJOR_REF)))
                    .tags(Collections.singletonList("Port Call Service - Consumer"))
                    .requestBody(
                        new RequestBody()
                            .description("Initiates a new or updates a **Port Call Service**.")
                            .required(true)
                            .content(
                                new Content()
                                    .addMediaType(
                                        DCSABase.JSON_CONTENT_TYPE,
                                        new MediaType()
                                            .schema(
                                                new Schema<>()
                                                    .$ref(
                                                        "#/components/schemas/PortCallService")))))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "204",
                                new ApiResponse()
                                    .description("A new Port Call Service accepted")
                                    .headers(API_VERSION_HEADER))
                            .addApiResponse(
                                "400",
                                new ApiResponse()
                                    .description(
                                        "In case creating a new **Port Call Service** fails schema validation, a `400` (Bad Request) is returned.")
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
                                        "In case creating a new or updating a **Port Call Service** that is linked to a **Terminal Call** that has been `OMITTED`, a `409` (Conflict) is returned.")
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
