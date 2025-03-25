package org.dcsa.conformance.standards.jit.schema.endpoints;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.JitSchemaComponents;
import org.dcsa.conformance.standards.jit.schema.JitSchemaCreator;
import org.dcsa.conformance.standards.jit.schema.common.DCSABase;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetPortCallsEndPoint {

  public static void addEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "/port-calls",
        new PathItem()
            .get(
                new Operation()
                    .summary("Retrieves a list of Port Calls")
                    .description(
                        """
Retrieves a list of **Port Calls** that match the specified filter criteria. It is **mandatory** to provide at least 1 filter.

The result set of this endPoint will always include **Port Calls** that have **not yet been completed**. Definition of a completed **Port Call** is:
````
A **Port Call**:
- that has a **Terminal Call**, and
- that has a **Port Call Service** with: `portCallServiceTypeCode=BERTH` AND `portCallServiceEventTypeCode=DEPA`, and
- that has a **Timestamp** with: `classifierCode=PLN` AND `dateTime < {now}`
````
This can potentially result in empty result sets. **Example:** filtering by `portCallID` that has been completed.

Here are some example queries:
 * To get a specific **Port Call** use the `portCallID` filter with the ID of the **Port Call** to filter by. This results in at most a single object. The response will return an empty array if no **Port Call** known by the **Service Consumer** having the provided `portCallID`.
 * To get a list of not completed **Port Calls** for a specific **VesselIMONumber**, use the `vesselIMONumber` filter with the `vesselIMONumber` of the **Vessel** to filter by. This will result in a list of potentially many **Port Calls** all of which will be visited by the **Vessel** with the `vesselIMONumber` specified.
 * To get a list of  not completed **Port Calls** for a specific **UN Location Code**, use the `UNLocationCode` filter with the `UNLocationCode` of the location to filter by. This will result in a list of potentially many **Port Calls** all of which will be located in the `UNLocationCode` specified.

**Note:** Beware it is possible to specify filters that exclude all results. Example: if filtering by `vesselIMONumber` and `MMSINumber` **not** belonging to the same **Vessel** - the result set will be an empty list. No error will be reported.
""")
                    .operationId("get-port-call")
                    .parameters(
                        List.of(
                            new Parameter().$ref("#/components/parameters/portCallIDQueryParam"),
                            new Parameter()
                                .$ref("#/components/parameters/portVisitReferenceQueryParam"),
                            new Parameter()
                                .$ref("#/components/parameters/UNLocationCodeQueryParam"),
                            new Parameter()
                                .$ref("#/components/parameters/vesselIMONumberQueryParam"),
                            new Parameter().$ref("#/components/parameters/vesselNameQueryParam"),
                            new Parameter().$ref("#/components/parameters/MMSINumberQueryParam"),
                            new Parameter().$ref(JitSchemaComponents.API_VERSION_MAJOR_REF),
                            new Parameter().$ref(JitSchemaComponents.REQUEST_SENDING_PARTY_REF),
                            new Parameter().$ref(JitSchemaComponents.REQUEST_RECEIVING_PARTY_REF)))
                    .tags(Collections.singletonList("Port Call Service"))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "200",
                                new ApiResponse()
                                    .description(
                                        "Retrieve a list of **Port Calls** that match the specified filter criteria.")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(
                                                        new Schema<>()
                                                            .$ref(
                                                                "#/components/schemas/PortCalls")))))
                            .addApiResponse(
                                "500",
                                new ApiResponse()
                                    .description(
                                        "In case a server error occurs in implementer system, a `500` (Internal Server Error) is returned.")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(DCSABase.getErrorResponseSchema()))))
                            .addApiResponse(
                                "default",
                                new ApiResponse()
                                    .description("Unexpected error")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .examples(
                                                        Map.of(
                                                            "conflictRequestExample",
                                                            new Example()
                                                                .summary(
                                                                    "Making too many Port Call requests")
                                                                .description(
                                                                    """
Calling the endPoint

    GET /port-calls

too many times within a time period results in an error.

**NB**: The `errorCode` is not yet standardized by DCSA. The value `7003` is just a "random example".
""")
                                                                .value(createTooManyRequest())))
                                                    .schema(DCSABase.getErrorResponseSchema()))))
                            .addApiResponse("500", JitSchemaCreator.getErrorApiResponse())
                            .addApiResponse(
                                "default",
                                new ApiResponse()
                                    .description("Unexpected error")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(
                                                        DCSABase.getErrorResponseSchema())))))));
  }

  private static LinkedHashMap<String, Object> createTooManyRequest() {
    var map = new LinkedHashMap<String, Object>();
    map.put("httpMethod", "GET");
    map.put("requestUri", "/port-calls");
    map.put("statusCode", 429);
    map.put("statusCodeText", "Too Many Requests");
    map.put(
        "statusCodeMessage",
        "Too many requests to fetch Port Calls has been requested. Please try again in 1 hour");
    map.put("providerCorrelationReference", "4426d965-0dd8-4005-8c63-dc68b01c4962");
    map.put("errorDateTime", "2024-09-04T09:41:00Z");
    map.put(
        "errors",
        Map.of(
            "errorCode",
            7003,
            "errorCodeText",
            "Max Port Call requests reached",
            "errorCodeMessage",
            "A maximum of 500 Port Calls can be requested per hour"));
    return map;
  }
}
