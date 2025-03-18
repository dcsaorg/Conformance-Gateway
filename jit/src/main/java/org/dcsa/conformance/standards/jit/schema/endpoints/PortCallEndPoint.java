package org.dcsa.conformance.standards.jit.schema.endpoints;

import static org.dcsa.conformance.standards.jit.schema.JitSchema.API_VERSION_HEADER;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.JitSchema;
import org.dcsa.conformance.standards.jit.schema.SchemaParams;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PortCallEndPoint {

  public static void addPortCallEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "'/port-calls/{portCallID}'",
        new PathItem()
            .put(
                new Operation()
                    .summary("Initiates a new or updates a Port Call")
                    .description(
                        """
Creates or updates a **Port Call** record. The caller must provide a unique `portCallID` (UUIDv4), which identifies the **Port Call**. The `portCallID` must remain consistent across all subsequent communications and linked **Terminal Calls**. If updating an existing **Port Call**, e.g. including the `portVisitReference`, the provided `portCallID` must match the existing record.

The **Port Call** includes:

- Location information (required): `UNLocationCode`
- static **Vessel** information (required): `vessel`
- an optional business identifier for the port visit: `portVisitReference`
- The ability to send the record with informational purpose only, using `isFYI=true`

This call is often provided as the first call from a **Carrier** to a **Terminal** before creating a **Terminal Call** and then sending `ETA-Berth` or `Moves`.

It is not possible to update a **Port Call** that has been `OMITTED`.
""")
                    .operationId("put-port-call")
                    .parameters(
                        List.of(
                            new Parameter().$ref(SchemaParams.PORT_CALL_ID_REF),
                            new Parameter().$ref(SchemaParams.API_VERSION_MAJOR_REF)))
                    .tags(Collections.singletonList("Port Call Service - Consumer"))
                    .requestBody(
                        new RequestBody()
                            .description("Initiates a new or updates a Port Call")
                            .required(true)
                            .content(
                                new Content()
                                    .addMediaType(
                                        JitSchema.JSON_CONTENT_TYPE,
                                        new MediaType()
                                            .schema(
                                                new Schema<>()
                                                    .$ref("#/components/schemas/PortCall")))))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "204",
                                new ApiResponse()
                                    .description(
                                        "A new or updated Port Call successfully accepted by the consumer.")
                                    .headers(API_VERSION_HEADER))
                            .addApiResponse(
                                "400",
                                new ApiResponse()
                                    .description(
                                        "In case creating a new **Port Call** fails schema validation, a `400` (Bad Request) is returned.")
                                    .headers(API_VERSION_HEADER)
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                JitSchema.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(
                                                        new Schema<>()
                                                            .$ref(
                                                                "#/components/schemas/ErrorResponse")))))
                            .addApiResponse(
                                "409",
                                new ApiResponse()
                                    .description(
                                        "In case updating a **Port Call** that has been `OMITTED`, a `409` (Conflict) is returned.")
                                    .headers(API_VERSION_HEADER)
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                JitSchema.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .examples(
                                                        Map.of(
                                                            "conflictRequestExample",
                                                            new Example()
                                                                .summary(
                                                                    "Updating a Port Call that is OMITTED")
                                                                .description(
                                                                    """
Updating an OMITTED **Port Call**, returns a `409` (Conflict)

**NB**: `errorCode` not yet standardized by DCSA. Value `7003` is just a "random example".
""")
                                                                .value(
                                                                    Map.of(
                                                                        "httpMethod",
                                                                        "PUT",
                                                                        "requestUri",
                                                                        "/port-calls/085a3207-5e45-49cf-8e1b-f8442beaf545",
                                                                        "statusCode",
                                                                        409,
                                                                        "statusCodeText",
                                                                        "Conflict",
                                                                        "statusCodeMessage",
                                                                        "Trying to update a Port Call that has been OMITTED",
                                                                        "providerCorrelationReference",
                                                                        "4426d965-0dd8-4005-8c63-dc68b01c4962",
                                                                        "errorDateTime",
                                                                        "2024-11-21T09:41:00Z",
                                                                        "errors",
                                                                        List.of(
                                                                            Map.of(
                                                                                "errorCode",
                                                                                7003,
                                                                                "errorCodeText",
                                                                                "Updating OMITTED Port Call",
                                                                                "errorCodeMessage",
                                                                                "Cannot update a Port Call that has been OMITTED"))))))
                                                    .schema(
                                                        new Schema<>()
                                                            .$ref(
                                                                "#/components/schemas/ErrorResponse")))))
                            .addApiResponse("500", JitSchema.getErrorApiResponse()))));
  }
}
