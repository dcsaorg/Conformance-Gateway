package org.dcsa.conformance.standards.an.schema;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetArrivalNoticesEndpoint {

  public static void addEndPoint(OpenAPI openAPI) {
    openAPI.path(
        "/arrival-notices",
        new PathItem()
            .get(
                new Operation()
                    .summary("Retrieves a list of Arrival Notices")
                    .description("TODO description")
                    .operationId("get-arrival-notices")
                    .tags(Collections.singletonList("Arrival Notices"))
                    .responses(
                        new ApiResponses()
                            .addApiResponse(
                                "200",
                                new ApiResponse()
                                    .description(
                                        "Retrieve a list of **Arrival Notices** that match the specified filter criteria.")
                                    .headers(
                                        new LinkedHashMap<>(
                                            Map.ofEntries(
                                                Map.entry(
                                                    "API-Version",
                                                    new Header()
                                                        .$ref(
                                                            "#/components/headers/API-Version")))))
                                    .content(
                                        new Content()
                                            .addMediaType(
                                                "application/json",
                                                new MediaType()
                                                    .schema(
                                                        new Schema<>()
                                                            .$ref(
                                                                "#/components/schemas/ArrivalNotice"))))))));
  }
}
