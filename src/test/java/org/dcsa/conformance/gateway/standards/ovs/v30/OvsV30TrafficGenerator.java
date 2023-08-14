package org.dcsa.conformance.gateway.standards.ovs.v30;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.GeneratedTrafficExchange;
import org.dcsa.conformance.gateway.TrafficGenerator;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class OvsV30TrafficGenerator implements TrafficGenerator {
  @Override
  public Stream<GeneratedTrafficExchange> get() {
    log.info("#############################################");
    log.info("################ ALL FILTERS ################");
    log.info("#############################################");
    OvsRequestFilter.allFilterTypes()
        .map(filter -> filter.asEntryStream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&")))
        .forEach(System.out::println);
    log.info("#############################################");
    Stream.of(
            GeneratedTrafficExchange.builder()
                    .requestPath("/v3/service-schedules?carrierServiceCode=AAA")
                    .requestBody("{\"RequestBodyKey\": \"RequestBodyValue\"}")
                    .responseBody("{\"MockResponseFor\": \"carrierServiceCode=AAA\"}")
                    .build(),
            GeneratedTrafficExchange.builder()
                    .requestPath("/v3/service-schedules?vesselIMONumber=1111111")
                    .requestBody("{\"RequestBodyKey\": \"RequestBodyValue\"}")
                    .responseBody("{\"MockResponseFor\": \"vesselIMONumber=1111111\"}")
                    .build())
            .limit(111);
    return Stream.of(
            null,
            OvsRequestFilter.builder(),
            null)
        .filter(Objects::nonNull)
            .flatMap(builder -> Stream.of(
                    builder,
                    builder
            ))
        .map(builder -> generateExchange(builder.build()));
  }

  private GeneratedTrafficExchange generateExchange(
          OvsRequestFilter requestFilter
  ) {
    return GeneratedTrafficExchange.builder()
            .requestPath("/v3/service-schedules?carrierServiceCode=AAA")
            .requestBody("{\"RequestBodyKey\": \"RequestBodyValue\"}")
            .responseBody("{\"MockResponseFor\": \"carrierServiceCode=AAA\"}")
            .build();
  }

  private static interface SubFilter {
    Stream<Map.Entry<String, Optional<String>>> asEntryStream();
  }

  @Builder
  private static class OvsRequestServiceSubFilter implements SubFilter {
    private String carrierServiceCode;
    private String universalServiceReference;

    @Override
    public Stream<Map.Entry<String, Optional<String>>> asEntryStream() {
      return Stream.of(
              Map.entry("carrierServiceCode", Optional.ofNullable(carrierServiceCode)),
              Map.entry("universalServiceReference", Optional.ofNullable(universalServiceReference))
      );
    }

    public static Stream<OvsRequestServiceSubFilter> allSubFilterTypes(boolean includingEmpty) {
      return Stream.of(
              OvsRequestServiceSubFilter.builder(),
              OvsRequestServiceSubFilter.builder()
                      .carrierServiceCode("csc"),
              OvsRequestServiceSubFilter.builder()
                      .universalServiceReference("usr"),
              OvsRequestServiceSubFilter.builder()
                      .carrierServiceCode("csc")
                      .universalServiceReference("usr")
      ).filter(builder -> includingEmpty
              || builder.carrierServiceCode != null
              || builder.universalServiceReference != null
      ).map(OvsRequestServiceSubFilterBuilder::build);
    }
  }

  @Builder
  private static class OvsRequestVesselSubFilter implements SubFilter {
    private String vesselImoNumber;
    private String vesselName;

    @Override
    public Stream<Map.Entry<String, Optional<String>>> asEntryStream() {
      return Stream.of(
              Map.entry("vesselIMONumber", Optional.ofNullable(vesselImoNumber)),
              Map.entry("vesselName", Optional.ofNullable(vesselName))
      );
    }

    public static Stream<OvsRequestVesselSubFilter> allSubFilterTypes(boolean includingEmpty) {
      return Stream.of(
              OvsRequestVesselSubFilter.builder(),
              OvsRequestVesselSubFilter.builder()
                      .vesselImoNumber("vin"),
              OvsRequestVesselSubFilter.builder()
                      .vesselName("vn"),
              OvsRequestVesselSubFilter.builder()
                      .vesselImoNumber("vin")
                      .vesselName("vn")
      ).filter(builder -> includingEmpty
              || builder.vesselImoNumber != null
              || builder.vesselName != null
      ).map(OvsRequestVesselSubFilterBuilder::build);
    }
  }

  @Builder
  private static class OvsRequestVoyageSubFilter implements SubFilter {
    private String carrierVoyageNumber;
    private String universalVoyageReference;

    @Override
    public Stream<Map.Entry<String, Optional<String>>> asEntryStream() {
      return Stream.of(
              Map.entry("carrierVoyageNumber", Optional.ofNullable(carrierVoyageNumber)),
              Map.entry("universalServiceReference", Optional.ofNullable(universalVoyageReference))
      );
    }

    public static Stream<OvsRequestVoyageSubFilter> allSubFilterTypes(boolean includingEmpty) {
      return Stream.of(
              OvsRequestVoyageSubFilter.builder(),
              OvsRequestVoyageSubFilter.builder()
                      .carrierVoyageNumber("cvn"),
              OvsRequestVoyageSubFilter.builder()
                      .universalVoyageReference("uvr"),
              OvsRequestVoyageSubFilter.builder()
                      .carrierVoyageNumber("cvn")
                      .universalVoyageReference("uvr")
      ).filter(builder -> includingEmpty
              || builder.carrierVoyageNumber != null
              || builder.universalVoyageReference != null
      ).map(OvsRequestVoyageSubFilterBuilder::build);
    }
  }

  @Builder
  private static class OvsRequestPortSubFilter implements SubFilter {
    private String unLocationCode;
    private String facilitySmdgCode;

    @Override
    public Stream<Map.Entry<String, Optional<String>>> asEntryStream() {
      return Stream.of(
              Map.entry("UNLocationCode", Optional.ofNullable(unLocationCode)),
              Map.entry("facilitySMDGCode", Optional.ofNullable(facilitySmdgCode))
      );
    }

    public static Stream<OvsRequestPortSubFilter> allSubFilterTypes(boolean includingEmpty) {
      return Stream.of(
              OvsRequestPortSubFilter.builder(),
              OvsRequestPortSubFilter.builder()
                      .unLocationCode("ulc"),
              OvsRequestPortSubFilter.builder()
                      .facilitySmdgCode("fsc"),
              OvsRequestPortSubFilter.builder()
                      .unLocationCode("ulc")
                      .facilitySmdgCode("fsc")
      ).filter(builder -> includingEmpty
              || builder.unLocationCode != null
              || builder.facilitySmdgCode != null
      ).map(OvsRequestPortSubFilterBuilder::build);
    }
  }

  @Builder
  private static class OvsRequestDatesSubFilter implements SubFilter {
    private String startDate;
    private String endDate;

    @Override
    public Stream<Map.Entry<String, Optional<String>>> asEntryStream() {
      return Stream.of(
              Map.entry("startDate", Optional.ofNullable(startDate)),
              Map.entry("endDate", Optional.ofNullable(endDate))
      );
    }

    public static Stream<OvsRequestDatesSubFilter> allSubFilterTypes(boolean includingEmpty) {
      return Stream.of(
              OvsRequestDatesSubFilter.builder(),
              OvsRequestDatesSubFilter.builder()
                      .startDate("2023-01-01"),
              OvsRequestDatesSubFilter.builder()
                      .endDate("2023-12-31"),
              OvsRequestDatesSubFilter.builder()
                      .startDate("2023-01-01")
                      .endDate("2023-12-31")
      ).filter(builder -> includingEmpty
              || builder.startDate != null
              || builder.endDate != null
      ).map(OvsRequestDatesSubFilterBuilder::build);
    }
  }

  @Builder
  private static class OvsRequestFilter {
    @Getter private OvsRequestServiceSubFilter serviceSubFilter;
    @Getter private OvsRequestVesselSubFilter vesselSubFilter;
    @Getter private OvsRequestVoyageSubFilter voyageSubFilter;
    @Getter private OvsRequestPortSubFilter portSubFilter;
    @Getter private OvsRequestDatesSubFilter datesSubFilter;
    // TODO limit, cursor, API-Version

    public Stream<Map.Entry<String, String>> asEntryStream() {
      return Stream.of(
              serviceSubFilter,
              vesselSubFilter,
              voyageSubFilter,
              portSubFilter,
              datesSubFilter
      )
              .filter(Objects::nonNull)
              .flatMap(SubFilter::asEntryStream)
              .filter(e -> e.getValue().isPresent())
              .map(e -> Map.entry(e.getKey(), e.getValue().orElse(null)));
    }

    private static Stream<OvsRequestFilter> allServiceFilterTypes() {
      return Stream.of(OvsRequestFilter.builder())
              // mandatory service
              .flatMap(requestBuilder -> OvsRequestServiceSubFilter.allSubFilterTypes(false).map(
                      serviceSubFilter -> requestBuilder.serviceSubFilter(serviceSubFilter)))
              // optional dates
              .flatMap(requestBuilder -> OvsRequestDatesSubFilter.allSubFilterTypes(true).map(
                      datesSubFilter -> requestBuilder.datesSubFilter(datesSubFilter)))
              .map(OvsRequestFilterBuilder::build);
    }

    private static Stream<OvsRequestFilter> allVoyageFilterTypes() {
      return Stream.of(OvsRequestFilter.builder())
              // mandatory service
              .flatMap(requestBuilder -> OvsRequestServiceSubFilter.allSubFilterTypes(false).map(
                      serviceSubFilter -> requestBuilder.serviceSubFilter(serviceSubFilter)))
              // mandatory voyage
              .flatMap(requestBuilder -> OvsRequestVoyageSubFilter.allSubFilterTypes(false).map(
                      voyageSubFilter -> requestBuilder.voyageSubFilter(voyageSubFilter)))
              // optional dates
              .flatMap(requestBuilder -> OvsRequestDatesSubFilter.allSubFilterTypes(true).map(
                      datesSubFilter -> requestBuilder.datesSubFilter(datesSubFilter)))
              .map(OvsRequestFilterBuilder::build);
    }

    private static Stream<OvsRequestFilter> allVesselFilterTypes() {
      return Stream.of(OvsRequestFilter.builder())
              // optional service
              .flatMap(requestBuilder -> OvsRequestServiceSubFilter.allSubFilterTypes(true).map(
                      serviceSubFilter -> requestBuilder.serviceSubFilter(serviceSubFilter)))
              // mandatory vessel
              .flatMap(requestBuilder -> OvsRequestVesselSubFilter.allSubFilterTypes(false).map(
                      vesselSubFilter -> requestBuilder.vesselSubFilter(vesselSubFilter)))
              // optional voyage
              .flatMap(requestBuilder -> OvsRequestVoyageSubFilter.allSubFilterTypes(true).map(
                      voyageSubFilter -> requestBuilder.voyageSubFilter(voyageSubFilter)))
              // optional dates
              .flatMap(requestBuilder -> OvsRequestDatesSubFilter.allSubFilterTypes(true).map(
                      datesSubFilter -> requestBuilder.datesSubFilter(datesSubFilter)))
              .map(OvsRequestFilterBuilder::build);
    }

    private static Stream<OvsRequestFilter> allPortFilterTypes() {
      return Stream.of(OvsRequestFilter.builder())
              // optional service
              .flatMap(requestBuilder -> OvsRequestServiceSubFilter.allSubFilterTypes(true).map(
                      serviceSubFilter -> requestBuilder.serviceSubFilter(serviceSubFilter)))
              // optional vessel
              .flatMap(requestBuilder -> OvsRequestVesselSubFilter.allSubFilterTypes(true).map(
                      vesselSubFilter -> requestBuilder.vesselSubFilter(vesselSubFilter)))
              // optional voyage
              .flatMap(requestBuilder -> OvsRequestVoyageSubFilter.allSubFilterTypes(true).map(
                      voyageSubFilter -> requestBuilder.voyageSubFilter(voyageSubFilter)))
              // mandatory port
              .flatMap(requestBuilder -> OvsRequestPortSubFilter.allSubFilterTypes(false).map(
                      portSubFilter -> requestBuilder.portSubFilter(portSubFilter)))
              // optional dates
              .flatMap(requestBuilder -> OvsRequestDatesSubFilter.allSubFilterTypes(true).map(
                      datesSubFilter -> requestBuilder.datesSubFilter(datesSubFilter)))
              .map(OvsRequestFilterBuilder::build);
    }

    public static Stream<OvsRequestFilter> allFilterTypes() {
      return Stream.of(
              Stream.of(OvsRequestFilter.builder().build()),
              allServiceFilterTypes(),
              allVoyageFilterTypes(),
              allVesselFilterTypes(),
              allPortFilterTypes()
      ).flatMap(f -> f);
    }
  }
}

/*
  SpringBootTest properties
            "gateway.targetUrl=http://localhost:${wiremock.server.port}",
            "gateway.links[0].sourceParty.name=Carrier C",
            "gateway.links[0].targetParty.name=Carrier K",
            "gateway.links[0].gatewayBasePath=/link0/gateway",
            "gateway.links[0].targetBasePath=/link0/target",
            "gateway.links[0].targetRootUrl=http://localhost:${wiremock.server.port}",
            "gateway.links[1].sourceParty.name=Carrier K",
            "gateway.links[1].targetParty.name=Carrier C",
            "gateway.links[1].gatewayBasePath=/link1/gateway",
            "gateway.links[1].targetBasePath=/link1/target",
            "gateway.links[1].targetRootUrl=http://localhost:${wiremock.server.port}",
            "gateway.links[2].sourceParty.name=Feeder F",
            "gateway.links[2].targetParty.name=Carrier C",
            "gateway.links[2].gatewayBasePath=/link2/gateway",
            "gateway.links[2].targetBasePath=/link2/target",
            "gateway.links[2].targetRootUrl=http://localhost:${wiremock.server.port}",
*/