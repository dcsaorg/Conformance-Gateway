package org.dcsa.conformance.standards.jit.schema.endpoints;

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
import org.dcsa.conformance.standards.jit.schema.JitSchemaComponents;
import org.dcsa.conformance.standards.jit.schema.JitSchemaCreator;
import org.dcsa.conformance.standards.jit.schema.common.DCSABase;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PortCallOmitEndPoint {

  public static void addEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "/port-calls/{portCallID}/omit",
        new PathItem()
            .put(
                new Operation()
                    .summary("Omits a Port Call")
                    .description(
                        """
Allows the **Service Provider** to `OMIT` a **Port Call**, signaling that the **Port Call** is no longer going to happen.

When a **Service Consumer** receives an `OMIT`, it is their responsibility to propagate this information to any secondary receivers, they previously informed using the `isFYI=true` property, while creating **Port Calls** or sending related updates.

The **Service Provider** is responsible for:
 - sending an `OMIT` to all **Terminal Calls** linked to the `portCallID` (including secondary receivers)
 - **Cancel** all **Port Call Services** that are associated with the omitted **Terminal Calls** and **Port Call**

The **Service Consumer** is responsible for:
 - propagating the `OMIT` to any secondary receivers
 - **Cancel** any **Port Call Services** linked to the omitted **Terminal Call** initiated by the **Service Consumer**

Once a **Port Call** has been `OMITTED`, this action **CANNOT** be undone. In case the `OMIT` has to be "undone" a new **Port Call** must be created with new **Terminal Calls** and new **Port Call Services**.
""")
                    .operationId("omit-port-call")
                    .parameters(
                        List.of(
                            new Parameter().$ref(JitSchemaComponents.PORT_CALL_ID_REF),
                            new Parameter().$ref(JitSchemaComponents.API_VERSION_MAJOR_REF),
                            new Parameter().$ref(JitSchemaComponents.REQUEST_SENDING_PARTY_REF),
                            new Parameter().$ref(JitSchemaComponents.REQUEST_RECEIVING_PARTY_REF)))
                    .tags(Collections.singletonList("Port Call Service - Service Consumer"))
                    .requestBody(
                        new RequestBody()
                            .description("Omits a **Port Call**")
                            .required(true)
                            .content(
                                new Content()
                                    .addMediaType(
                                        DCSABase.JSON_CONTENT_TYPE,
                                        new MediaType()
                                            .schema(
                                                new Schema<>()
                                                    .$ref("#/components/schemas/OmitPortCall")))))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "204",
                                new ApiResponse()
                                    .description("**Port Call** successfully marked as omitted.")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders()))
                            .addApiResponse(
                                "400",
                                new ApiResponse()
                                    .description(
                                        "In case omitting a **Port Call** fails schema validation, a `400` (Bad Request) is returned.")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
                                                    .schema(DCSABase.getErrorResponseSchema()))))
                            .addApiResponse(
                                "404",
                                new ApiResponse()
                                    .description(
                                        "If the implementer does not know the `portCallID` path parameter (e.g. the resource does not exist), it is possible for the implementer to reject the request by returning a `404` (Not Found).")
                                    .headers(JitSchemaComponents.getDefaultJitHeaders())
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                DCSABase.JSON_CONTENT_TYPE,
                                                new MediaType()
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
}
