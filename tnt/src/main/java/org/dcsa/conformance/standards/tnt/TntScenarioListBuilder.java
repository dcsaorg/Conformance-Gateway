package org.dcsa.conformance.standards.tnt;

import static org.dcsa.conformance.standards.tnt.party.TntFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.tnt.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.action.TntAction;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsAction;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsBadRequestAction;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;

@Slf4j
class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {
  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
      TntComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "SHIPMENT",
                noAction()
                    .thenEither(
                        scenarioWithFilterByShipmentEvent(Map.of(SHIPMENT_EVENT_TYPE_CODE, "DRFT")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:00:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:01:56+01:00")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:02:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:22:56+01:00")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:14:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:12:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByShipmentEvent(Map.of(DOCUMENT_TYPE_CODE, "SHI")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(CARRIER_BOOKING_REFERENCE, "ABC123123123")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(TRANSPORT_DOCUMENT_REFERENCE, "reserved-HHL123")),
                        scenarioWithFilterByShipmentEvent(
                            Map.of(EQUIPMENT_REFERENCE, "APZU4812090")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "INVALID_SHIPMENT_EVENT")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(SHIPMENT_EVENT_TYPE_CODE, "INVALID_SHIPMENT_EVENT_TYPE_CODE")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(DOCUMENT_TYPE_CODE, "INVALID_DOCUMENT_TYPE_CODE")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "SHIPMENT", TRANSPORT_EVENT_TYPE_CODE, "ARRV")),
                        scenarioWithFilterByPagination(
                            getEvents(), Map.of(EVENT_TYPE, "SHIPMENT", LIMIT, "1")))),
            Map.entry(
                "TRANSPORT",
                noAction()
                    .thenEither(
                        scenarioWithFilterByTransportEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:13:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:12:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:14:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:16:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(CARRIER_BOOKING_REFERENCE, "ABC123123123")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(
                                TRANSPORT_DOCUMENT_REFERENCE,
                                "85943567-eedb-98d3-f4ed-aed697474ed4")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(TRANSPORT_EVENT_TYPE_CODE, "ARRI")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(TRANSPORT_CALL_ID, "123e4567-e89b-12d3-a456-426614174000")),
                        scenarioWithFilterByTransportEvent(Map.of(VESSEL_IMO_NUMBER, "9321483")),
                        scenarioWithFilterByTransportEvent(Map.of(EXPORT_VOYAGE_NUMBER, "2103S")),
                        scenarioWithFilterByTransportEvent(Map.of(CARRIER_SERVICE_CODE, "FE1")),
                        scenarioWithFilterByTransportEvent(Map.of(UN_LOCATION_CODE, "FRPAR")),
                        scenarioWithFilterByTransportEvent(
                            Map.of(EQUIPMENT_REFERENCE, "APZU4812090")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "INVALID_TRANSPORT_EVENT")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "TRANSPORT", SHIPMENT_EVENT_TYPE_CODE, "DRFT")),
                        scenarioWithFilterByPagination(
                            getEvents(), Map.of(EVENT_TYPE, "TRANSPORT", LIMIT, "1")))),
            Map.entry(
                "EQUIPMENT",
                noAction()
                    .thenEither(
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:13:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GT,
                                "2001-01-01T14:12:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:12:56+01:00",
                                EVENT_CREATED_DATE_TIME_LT,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(
                                EVENT_CREATED_DATE_TIME_GTE,
                                "2001-01-01T14:12:56+01:00",
                                EVENT_CREATED_DATE_TIME_LTE,
                                "2050-01-30T14:12:56+01:00")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(CARRIER_BOOKING_REFERENCE, "ABC123123123")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(
                                TRANSPORT_DOCUMENT_REFERENCE,
                                "85943567-eedb-98d3-f4ed-aed697474ed4")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(TRANSPORT_CALL_ID, "123e4567-e89b-12d3-a456-426614174000")),
                        scenarioWithFilterByEquipmentEvent(Map.of(VESSEL_IMO_NUMBER, "9321483")),
                        scenarioWithFilterByEquipmentEvent(Map.of(EXPORT_VOYAGE_NUMBER, "2103S")),
                        scenarioWithFilterByEquipmentEvent(Map.of(CARRIER_SERVICE_CODE, "FE1")),
                        scenarioWithFilterByEquipmentEvent(Map.of(UN_LOCATION_CODE, "FRPAR")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(EQUIPMENT_EVENT_TYPE_CODE, "LOAD")),
                        scenarioWithFilterByEquipmentEvent(
                            Map.of(EQUIPMENT_REFERENCE, "APZU4812090")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "INVALID_EQUIPMENT_EVENT")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EQUIPMENT_EVENT_TYPE_CODE, "INVALID_EQUIPMENT_TYPE_CODE")),
                        scenarioWithBadRequestFilterBy(
                            Map.of(EVENT_TYPE, "EQUIPMENT", SHIPMENT_EVENT_TYPE_CODE, "DRFT")),
                        scenarioWithFilterByPagination(
                            getEvents(), Map.of(EVENT_TYPE, "EQUIPMENT", LIMIT, "1")))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private TntScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static TntScenarioListBuilder noAction() {
    return new TntScenarioListBuilder(null);
  }

  private static TntScenarioListBuilder scenarioWithFilterByShipmentEvent(
      Map<TntFilterParameter, String> parameters) {
    Map<TntFilterParameter, String> shipmentParameters = new LinkedHashMap<>();
    shipmentParameters.put(EVENT_TYPE, "SHIPMENT");
    shipmentParameters.putAll(parameters);
    return supplyScenarioParameters(shipmentParameters).then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterByTransportEvent(
      Map<TntFilterParameter, String> parameters) {
    Map<TntFilterParameter, String> shipmentParameters = new LinkedHashMap<>();
    shipmentParameters.put(EVENT_TYPE, "TRANSPORT");
    shipmentParameters.putAll(parameters);
    return supplyScenarioParameters(shipmentParameters).then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterByEquipmentEvent(
      Map<TntFilterParameter, String> parameters) {
    Map<TntFilterParameter, String> shipmentParameters = new LinkedHashMap<>();
    shipmentParameters.put(EVENT_TYPE, "EQUIPMENT");
    shipmentParameters.putAll(parameters);
    return supplyScenarioParameters(shipmentParameters).then(getEvents());
  }

  private static TntScenarioListBuilder scenarioWithFilterByPagination(
      TntScenarioListBuilder nextEventsAction, Map<TntFilterParameter, String> parameters) {
    return supplyScenarioParameters(new LinkedHashMap<>(parameters))
        .then(nextEventsAction.then(getEvents()));
  }

  private static TntScenarioListBuilder supplyScenarioParameters(
      Map<TntFilterParameter, String> parameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new TntScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(publisherPartyName, parameters));
  }

  private static TntScenarioListBuilder scenarioWithBadRequestFilterBy(
      Map<TntFilterParameter, String> parameters) {
    return supplyScenarioParameters(parameters).then(getEventsBadRequest());
  }

  private static TntScenarioListBuilder getEvents() {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new TntGetEventsAction(
                subscriberPartyName,
                publisherPartyName,
                (TntAction) previousAction,
                componentFactory.getEventSchemaValidators()));
  }

  private static TntScenarioListBuilder getEventsBadRequest() {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new TntGetEventsBadRequestAction(
                subscriberPartyName,
                publisherPartyName,
                (TntAction) previousAction,
                componentFactory.getEventSchemaValidators()));
  }
}
