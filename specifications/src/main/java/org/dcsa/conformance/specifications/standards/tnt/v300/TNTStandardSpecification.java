package org.dcsa.conformance.specifications.standards.tnt.v300;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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
import org.dcsa.conformance.specifications.standards.core.v100.model.ClassifiedDateTime;
import org.dcsa.conformance.specifications.standards.core.v100.model.Facility;
import org.dcsa.conformance.specifications.standards.core.v100.model.GeoCoordinate;
import org.dcsa.conformance.specifications.standards.core.v100.model.Location;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.FeedbackElement;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.GetEventsError;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.GetEventsResponse;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.PostEventsError;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.PostEventsRequest;
import org.dcsa.conformance.specifications.standards.tnt.v300.messages.PostEventsResponse;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.AbstractEventDetails;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.DocumentReference;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.EquipmentEventDetails;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.Event;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.IotEventDetails;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.ReeferEventDetails;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.ShipmentEventDetails;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.ShipmentReference;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.TransportCall;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.TransportEventDetails;

public class TNTStandardSpecification extends StandardSpecification {

  public static final String TAG_EVENT_PUBLISHERS = "Event Publisher Endpoints";
  public static final String TAG_EVENT_SUBSCRIBERS = "Event Subscriber Endpoints";

  private final GetEventsEndpoint getEventsEndpoint;

  public TNTStandardSpecification() {
    super("Track and Trace", "TNT", "3.0.0");

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
    return new LegendMetadata("Track and Trace", "3.0.0-20250912-design", "", "", 4);
  }

  @Override
  protected Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        AbstractEventDetails.class,
        Address.class,
        ClassifiedDateTime.class,
        DocumentReference.class,
        EquipmentEventDetails.class,
        Event.class,
        Facility.class,
        FeedbackElement.class,
        GeoCoordinate.class,
        GetEventsError.class,
        GetEventsResponse.class,
        IotEventDetails.class,
        Location.class,
        PostEventsError.class,
        PostEventsRequest.class,
        PostEventsResponse.class,
        ReeferEventDetails.class,
        ShipmentEventDetails.class,
        ShipmentReference.class,
        TransportCall.class,
        TransportEventDetails.class);
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
                                "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/TBD/specifications/generated-resources/standards/ct/v300/ct-v3.0.0-data-overview-%s.csv"
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

  private Operation operationEventsGet() {
    return new Operation()
        .summary("Retrieves a list of events")
        .description(readResourceFile("openapi-get-events-description.md"))
        .operationId("get-events")
        .tags(Collections.singletonList(TAG_EVENT_PUBLISHERS))
        .parameters(new GetEventsEndpoint().getQueryParameters())
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("List of events matching the query parameters")
                        .headers(
                            Stream.of(
                                    Map.entry(
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")),
                                    Map.entry(
                                        "Next-Page-Cursor",
                                        new Header().$ref("#/components/headers/Next-Page-Cursor")))
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
                                        "API-Version",
                                        new Header().$ref("#/components/headers/API-Version")))))
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
                    Map.entry(
                        "API-Version", new Header().$ref("#/components/headers/API-Version")))))
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
