package org.dcsa.conformance.specifications.standards.vgm.v100;

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
import org.dcsa.conformance.specifications.standards.core.v100.model.Party;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.FeedbackElement;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.GetVGMsError;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.GetVGMsResponse;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.PostVGMsError;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.PostVGMsRequest;
import org.dcsa.conformance.specifications.standards.vgm.v100.messages.PostVGMsResponse;
import org.dcsa.conformance.specifications.standards.vgm.v100.model.VGM;
import org.dcsa.conformance.specifications.standards.vgm.v100.model.VGMRouting;

public class VGMStandardSpecification extends StandardSpecification {

  public static final String TAG_VGM_PRODUCERS = "VGM Producer Endpoints";
  public static final String TAG_VGM_CONSUMERS = "VGM Consumer Endpoints";

  private final GetVGMsEndpoint getVGMsEndpoint;

  public VGMStandardSpecification() {
    super("Verified Gross Mass", "VGM", "1.0.0");

    openAPI.addTagsItem(
        new Tag()
            .name(TAG_VGM_PRODUCERS)
            .description("Endpoints implemented by the VGM Producers"));
    openAPI.addTagsItem(
        new Tag()
            .name(TAG_VGM_CONSUMERS)
            .description("Endpoints implemented by the VGM Consumers"));

    openAPI.path("/vgms", new PathItem().get(operationVGMsGet()).post(operationVGMsPost()));

    getVGMsEndpoint = new GetVGMsEndpoint();
  }

  @Override
  protected LegendMetadata getLegendMetadata() {
    return new LegendMetadata("Verified Gross Mass", "1.0.0-20251024-design", "VGM", "", 4);
  }

  @Override
  protected Stream<Class<?>> modelClassesStream() {
    return Stream.of(
        Address.class,
        ClassifiedDateTime.class,
        Facility.class,
        FeedbackElement.class,
        GeoCoordinate.class,
        GetVGMsError.class,
        GetVGMsResponse.class,
        Location.class,
        Party.class,
        PostVGMsError.class,
        PostVGMsRequest.class,
        PostVGMsResponse.class,
        VGM.class,
        VGMRouting.class);
  }

  @Override
  protected List<String> getRootTypeNames() {
    return List.of(VGM.class.getSimpleName());
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
                    System.currentTimeMillis() > 0
                        ? List.of()
                        : // TODO remove after first snapshot
                        DataOverviewSheet.importFromString(
                            SpecificationToolkit.readRemoteFile(
                                "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/TODO/specifications/generated-resources/standards/vgm/v100/vgm-v1.0.0-data-overview-%s.csv"
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
    return getVGMsEndpoint;
  }

  @Override
  protected boolean swapOldAndNewInDataOverview() {
    return false;
  }

  private Operation operationVGMsGet() {
    return new Operation()
        .summary("Retrieves a list of VGMs")
        .description(readResourceFile("openapi-get-vgms-description.md"))
        .operationId("get-vgms")
        .tags(Collections.singletonList(TAG_VGM_PRODUCERS))
        .parameters(
            Stream.concat(
                    new GetVGMsEndpoint().getQueryParameters().stream(),
                    Stream.of(getApiVersionHeaderParameter()))
                .toList())
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("List of VGMs matching the query parameters")
                        .headers(
                            Stream.of(
                                    Map.entry(
                                        API_VERSION_HEADER,
                                        new Header().$ref(API_VERSION_HEADER_REF)),
                                    Map.entry(
                                        NEXT_PAGE_CURSOR_HEADER,
                                        new Header().$ref(NEXT_PAGE_CURSOR_HEADER_REF)))
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
                                                        GetVGMsResponse.class))))))
                .addApiResponse("default", createErrorResponse(GetVGMsError.class)));
  }

  private Operation operationVGMsPost() {
    return new Operation()
        .summary("Sends a list of VGMs")
        .description(readResourceFile("openapi-post-vgms-description.md"))
        .operationId("post-vgms")
        .tags(Collections.singletonList(TAG_VGM_CONSUMERS))
        .parameters(List.of(getApiVersionHeaderParameter()))
        .requestBody(
            new RequestBody()
                .description("List of VGMs")
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
                                                PostVGMsRequest.class))))))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200",
                    new ApiResponse()
                        .description("VGMs response")
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
                                                        PostVGMsResponse.class))))))
                .addApiResponse("default", createErrorResponse(PostVGMsError.class)));
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
