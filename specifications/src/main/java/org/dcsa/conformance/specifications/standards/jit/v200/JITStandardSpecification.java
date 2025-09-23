package org.dcsa.conformance.specifications.standards.jit.v200;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.specifications.dataoverview.AttributesHierarchicalSheet;
import org.dcsa.conformance.specifications.dataoverview.AttributesNormalizedSheet;
import org.dcsa.conformance.specifications.dataoverview.DataOverviewSheet;
import org.dcsa.conformance.specifications.dataoverview.LegendMetadata;
import org.dcsa.conformance.specifications.dataoverview.QueryFiltersSheet;
import org.dcsa.conformance.specifications.dataoverview.QueryParametersSheet;
import org.dcsa.conformance.specifications.generator.QueryParametersFilterEndpoint;
import org.dcsa.conformance.specifications.generator.SpecificationToolkit;
import org.dcsa.conformance.specifications.generator.StandardSpecification;
import org.dcsa.conformance.specifications.standards.core.v100.model.Address;
import org.dcsa.conformance.specifications.standards.core.v100.model.Facility;
import org.dcsa.conformance.specifications.standards.core.v100.model.GeoCoordinate;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.FeedbackElement;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.GetEventsError;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.GetEventsResponse;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.PostEventsError;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.PostEventsRequest;
import org.dcsa.conformance.specifications.standards.jit.v200.messages.PostEventsResponse;
import org.dcsa.conformance.specifications.standards.jit.v200.model.ContainerCountBySize;
import org.dcsa.conformance.specifications.standards.jit.v200.model.ContainerCountByTypeAndSize;
import org.dcsa.conformance.specifications.standards.jit.v200.model.Event;
import org.dcsa.conformance.specifications.standards.jit.v200.model.MovesForecast;
import org.dcsa.conformance.specifications.standards.jit.v200.model.PortCall;
import org.dcsa.conformance.specifications.standards.jit.v200.model.PortCallService;
import org.dcsa.conformance.specifications.standards.jit.v200.model.TerminalCall;
import org.dcsa.conformance.specifications.standards.jit.v200.model.Vessel;

public class JITStandardSpecification extends StandardSpecification {

  private static final String TAG_EVENT_PUBLISHERS = "Event Publisher Endpoints";
  private static final String TAG_EVENT_SUBSCRIBERS = "Event Subscriber Endpoints";
  private static final String REQUEST_SENDING_PARTY_HEADER = "Request-Sending-Party";
  private static final String REQUEST_RECEIVING_PARTY_HEADER = "Request-Receiving-Party";
  private static final String RESPONSE_SENDING_PARTY_HEADER = "Response-Sending-Party";
  private static final String RESPONSE_RECEIVING_PARTY_HEADER = "Response-Receiving-Party";

  private final GetEventsEndpoint getEventsEndpoint;

  public JITStandardSpecification() {
    super("Just in Time Port Call", "JIT", "2.0.0");

    openAPI.addTagsItem(
        new Tag()
            .name(TAG_EVENT_PUBLISHERS)
            .description("Endpoints implemented by the adopters who publish events"));
    openAPI.addTagsItem(
        new Tag()
            .name(TAG_EVENT_SUBSCRIBERS)
            .description("Endpoints implemented by the adopters who receive events"));

    openAPI.path("/events", new PathItem().get(operationEventsGet()).post(operationEventsPost()));

    getEventsEndpoint = new GetEventsEndpoint();
  }

  @Override
  protected LegendMetadata getLegendMetadata() {
    return new LegendMetadata("Just in Time Port Call", "2.0.0-20250926-design", "", "", 4);
  }

  @Override
  protected Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        Address.class,
        ContainerCountBySize.class,
        ContainerCountByTypeAndSize.class,
        Event.class,
        Facility.class,
        FeedbackElement.class,
        GeoCoordinate.class,
        GetEventsError.class,
        GetEventsResponse.class,
        Location.class,
        MovesForecast.class,
        PortCall.class,
        PortCallService.class,
        PostEventsError.class,
        PostEventsRequest.class,
        PostEventsResponse.class,
        TerminalCall.class,
        Vessel.class);
  }

  @Override
  protected List<String> getRootTypeNames() {
    return List.of(Event.class.getSimpleName());
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, List<List<String>>>
      getOldDataValuesBySheetClass() {
    return Map.ofEntries(
            Map.entry(AttributesHierarchicalSheet.class, "attributes-hierarchical"),
            Map.entry(AttributesNormalizedSheet.class, "attributes-normalized"),
            Map.entry(QueryParametersSheet.class, "query-parameters"),
            Map.entry(QueryFiltersSheet.class, "query-filters"))
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    System.currentTimeMillis() > 0 // TODO remove for the second snapshot
                        ? List.of()
                        : DataOverviewSheet.importFromString(
                            SpecificationToolkit.readRemoteFile(
                                "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/TBD/specifications/generated-resources/standards/jit/v200/jit-v2.0.0-data-overview-%s.csv"
                                    .formatted(entry.getValue())))));
  }

  @Override
  protected Map<Class<? extends DataOverviewSheet>, Map<String, String>>
      getChangedPrimaryKeyByOldPrimaryKeyBySheetClass() {
    return Map.ofEntries(
        Map.entry(AttributesHierarchicalSheet.class, Map.ofEntries()),
        Map.entry(AttributesNormalizedSheet.class, Map.ofEntries()),
        Map.entry(QueryFiltersSheet.class, Map.ofEntries()),
        Map.entry(QueryParametersSheet.class, Map.ofEntries()));
  }

  protected QueryParametersFilterEndpoint getQueryParametersFilterEndpoint() {
    return getEventsEndpoint;
  }

  @Override
  protected boolean swapOldAndNewInDataOverview() {
    return false;
  }

  @Override
  protected Stream<Map.Entry<String, Header>> getCustomHeaders() {
    return Stream.concat(getRequestCustomHeaders(), getResponseCustomHeaders());
  }

  private Stream<Map.Entry<String, Header>> getRequestCustomHeaders() {
    return Stream.of(
        Map.entry(
            REQUEST_SENDING_PARTY_HEADER,
            new Header()
                .description(
"""
When communicating through an optional system that acts as an application level JIT communication proxy,
forwarding API calls between JIT **Service Providers** and JIT **Service Consumers**,
the API client sets this request header to identify itself to the JIT proxy and to the API server
as the original sending party of the API request.

The assignment of party identifiers by the JIT proxy and the distribution of identifiers
to the parties connecting through the JIT proxy are out of scope.
""")
                .schema(new Schema<>().type("string").example("Carrier-123"))),
        Map.entry(
            REQUEST_RECEIVING_PARTY_HEADER,
            new Header()
                .description(
"""
When communicating through an optional system that acts as an application level JIT communication proxy,
forwarding API calls between JIT **Service Providers** and JIT **Service Consumers**,
the API client sets this request header to identify to the JIT proxy the target receiving party of the API request.

The assignment of party identifiers by the JIT proxy and the distribution of identifiers
to the parties connecting through the JIT proxy are out of scope.
""")
                .schema(new Schema<>().type("string").example("Terminal-456"))));
  }

  private Stream<Map.Entry<String, Header>> getResponseCustomHeaders() {
    return Stream.of(
        Map.entry(
            RESPONSE_SENDING_PARTY_HEADER,
            new Header()
                .description(
"""
When communicating through an optional system that acts as an application level JIT communication proxy,
forwarding API calls between JIT **Service Providers** and JIT **Service Consumers**,
the API server sets this response header to identify itself to the JIT proxy and to the API client
as the original sending party of the API response.

The value of this response header must be the same as the value of the request header `Request-Receiving-Party`.

The assignment of party identifiers by the JIT proxy and the distribution of identifiers
to the parties connecting through the JIT proxy are out of scope.
""")
                .schema(new Schema<>().type("string").example("Terminal-456"))),
        Map.entry(
            RESPONSE_RECEIVING_PARTY_HEADER,
            new Header()
                .description(
"""
When communicating through an optional system that acts as an application level JIT communication proxy,
forwarding API calls between JIT **Service Providers** and JIT **Service Consumers**,
the API server sets this response header to identify to the JIT proxy the target receiving party of the API response.

The value of this response header must be the same as the value of the request header `Request-Sending-Party`.

The assignment of party identifiers by the JIT proxy and the distribution of identifiers
to the parties connecting through the JIT proxy are out of scope.
""")
                .schema(new Schema<>().type("string").example("Carrier-123"))));
  }

  private Operation operationEventsGet() {
    return new Operation()
        .summary("Retrieves a list of events")
        .description(readResourceFile("openapi-get-events-description.md"))
        .operationId("get-events")
        .tags(Collections.singletonList(TAG_EVENT_PUBLISHERS))
        .parameters(
            Stream.concat(
                    new GetEventsEndpoint().getQueryParameters().stream(),
                    Stream.concat(
                        Stream.of(getApiVersionHeaderParameter()),
                        getRequestCustomHeaders()
                            .map(
                                nameAndHeader ->
                                    new Parameter()
                                        .in("header")
                                        .name(nameAndHeader.getKey())
                                        .description(nameAndHeader.getValue().getDescription())
                                        .required(false)
                                        .schema(nameAndHeader.getValue().getSchema())
                                        .example(nameAndHeader.getValue().getExample()))))
                .toList())
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("List of events matching the query parameters")
                        .headers(
                            Stream.of(
                                    API_VERSION_HEADER,
                                    NEXT_PAGE_CURSOR_HEADER,
                                    RESPONSE_SENDING_PARTY_HEADER,
                                    RESPONSE_RECEIVING_PARTY_HEADER)
                                .map(
                                    headerName ->
                                        Map.entry(
                                            headerName,
                                            new Header()
                                                .$ref(COMPONENTS_HEADERS_REF_PATH + headerName)))
                                .collect(
                                    Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> b,
                                        LinkedHashMap::new)))
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref(
                                                    SpecificationToolkit.getComponentSchema$ref(
                                                        GetEventsResponse.class))))))
                .addApiResponse("default", createErrorResponse(GetEventsError.class)));
  }

  private Operation operationEventsPost() {
    return new Operation()
        .summary("Sends a list of events")
        .description(readResourceFile("openapi-post-events-description.md"))
        .operationId("post-events")
        .tags(Collections.singletonList(TAG_EVENT_SUBSCRIBERS))
        .requestBody(
            new RequestBody()
                .description("List of events")
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/json",
                            new MediaType()
                                .schema(
                                    new Schema<>()
                                        .$ref(
                                            SpecificationToolkit.getComponentSchema$ref(
                                                PostEventsRequest.class))))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("Events response")
                        .headers(
                            new LinkedHashMap<>(
                                Map.ofEntries(
                                    Map.entry(
                                        API_VERSION_HEADER,
                                        new Header().$ref(API_VERSION_HEADER_REF)))))
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref(
                                                    SpecificationToolkit.getComponentSchema$ref(
                                                        PostEventsResponse.class))))))
                .addApiResponse("default", createErrorResponse(PostEventsError.class)));
  }

  private ApiResponse createErrorResponse(Class<?> errorMessageClass) {
    return new ApiResponse()
        .description("Error response")
        .headers(
            new LinkedHashMap<>(
                Map.ofEntries(
                    Map.entry(API_VERSION_HEADER, new Header().$ref(API_VERSION_HEADER_REF)))))
        .content(
            new Content()
                .addMediaType(
                    "application/json",
                    new MediaType()
                        .schema(
                            new Schema<>()
                                .$ref(
                                    SpecificationToolkit.getComponentSchema$ref(
                                        errorMessageClass)))));
  }
}
