package org.dcsa.conformance.standards.an;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.an.action.ANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANNotificationAction;
import org.dcsa.conformance.standards.an.action.SubscriberGetANAction;
import org.dcsa.conformance.standards.an.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.ANFilterParameter;
import org.dcsa.conformance.standards.an.party.ANRole;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class ANScenarioListBuilder extends ScenarioListBuilder<ANScenarioListBuilder> {

  private static final ThreadLocal<ANComponentFactory> threadLocalComponentFactory = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  private ANScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, ANScenarioListBuilder> createModuleScenarioListBuilders(
    ANComponentFactory anComponentFactory,
    Set<String> testedPartyRoleNames,
    String producerPartyName,
    String consumerPartyName) {

    threadLocalComponentFactory.set(anComponentFactory);
    threadLocalPublisherPartyName.set(producerPartyName);
    threadLocalSubscriberPartyName.set(consumerPartyName);

    Map<String, Map<String, ANScenarioListBuilder>> scenariosByRole =
      MapUtils.orderedMap(
        Map.entry(ANRole.PRODUCER.getConfigName(), producerScenarios()),
        Map.entry(ANRole.CONSUMER.getConfigName(), consumerScenarios()));
    return MapUtils.mergePartyScenarioModules(scenariosByRole, testedPartyRoleNames);
  }

  private static Map<String, ANScenarioListBuilder> producerScenarios() {
    LinkedHashMap<String, ANScenarioListBuilder> modules = new LinkedHashMap<>();
    modules.put(
      "AN Producer: GET scenarios for the required query parameter filter — Required",
      noAction()
        .thenEither(
          requiredGetScenario(ScenarioType.BASIC),
          requiredGetScenario(ScenarioType.FREIGHTED),
          requiredGetScenario(ScenarioType.FREE_TIME))
        .asInterchangeableScenarios());
    modules.put(
      "AN Producer: GET scenario for optional query parameter filters — Optional/report-only",
      supplyScenarioParameters(
        ScenarioType.BASIC,
        new ANFilterParameter[0],
        optionalFilters(),
        "Supply parameters")
        .then(getArrivalNotices("GET Arrival Notice", false))
        .asOptionalReportOnlyScenario());
    modules.put(
      "AN Producer: GET scenario for pagination — Optional/report-only",
      supplyScenarioParameters(
        ScenarioType.BASIC,
        new ANFilterParameter[]{
          ANFilterParameter.TRANSPORT_DOCUMENT_REFERENCES, ANFilterParameter.LIMIT
        },
        new ANFilterParameter[0],
        "Supply parameters (transportDocumentReferences + limit)")
        .then(
          getArrivalNotices("GET Arrival Notice", true)
            .then(
              getArrivalNotices(
                "GET Arrival Notice (transportDocumentReferences + limit + cursor)",
                false)))
        .asOptionalReportOnlyScenario());
    modules.put(
      "AN Producer: POST scenarios for full arrival notices — Optional/report-only",
      noAction()
        .thenEither(
          postArrivalNotices(ScenarioType.BASIC, "POST Arrival Notice (BASIC)", true),
          postArrivalNotices(ScenarioType.FREIGHTED, "POST Arrival Notice (FREIGHTED)", true),
          postArrivalNotices(ScenarioType.FREE_TIME, "POST Arrival Notice (FREE_TIME)", true))
        .asOptionalReportOnlyScenario());
    modules.put(
      "AN Producer: POST scenario for arrival notice notifications — Optional/report-only",
      withScenarioTitlePrefix(
        postArrivalNoticesNotification("POST Arrival Notice Notification"), "AN Producer: ")
        .asOptionalReportOnlyScenario());
    return modules;
  }

  private static Map<String, ANScenarioListBuilder> consumerScenarios() {
    LinkedHashMap<String, ANScenarioListBuilder> modules = new LinkedHashMap<>();
    modules.put(
      "AN Consumer: GET scenario — Required",
      noAction().then(getArrivalNotices("GET Arrival Notice", false)));
    modules.put(
      "AN Consumer: POST scenarios — Optional/report-only",
      noAction()
        .thenEither(
          postArrivalNotices(ScenarioType.BASIC, "POST Arrival Notice", false),
          postArrivalNoticesNotification("POST Arrival Notice Notification"))
        .asOptionalReportOnlyScenario());
    return modules;
  }

  private static ANScenarioListBuilder requiredGetScenario(ScenarioType scenarioType) {
    return supplyScenarioParameters(
      scenarioType,
      new ANFilterParameter[]{ANFilterParameter.TRANSPORT_DOCUMENT_REFERENCES},
      new ANFilterParameter[0],
      "Supply parameters (transportDocumentReferences)")
      .then(getArrivalNotices("GET Arrival Notice (%s)".formatted(scenarioType.name()), false));
  }

  private static ANFilterParameter[] optionalFilters() {
    return new ANFilterParameter[]{
      ANFilterParameter.EQUIPMENT_REFERENCES,
      ANFilterParameter.PORT_OF_DISCHARGE,
      ANFilterParameter.VESSEL_IMO_NUMBER,
      ANFilterParameter.VESSEL_NAME,
      ANFilterParameter.CARRIER_IMPORT_VOYAGE_NUMBER,
      ANFilterParameter.UNIVERSAL_IMPORT_VOYAGE_REFERENCE,
      ANFilterParameter.CARRIER_SERVICE_CODE,
      ANFilterParameter.UNIVERSAL_SERVICE_REFERENCE,
      ANFilterParameter.PORT_OF_DISCHARGE_ARRIVAL_DATE_MIN,
      ANFilterParameter.PORT_OF_DISCHARGE_ARRIVAL_DATE_MAX
    };
  }

  private static ANScenarioListBuilder supplyScenarioParameters(
    ScenarioType scenarioType,
    ANFilterParameter[] requiredParameters,
    ANFilterParameter[] optionalParameters,
    String title) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new ANScenarioListBuilder(
      previousAction ->
        new SupplyScenarioParametersAction(
          publisherPartyName,
          scenarioType,
          requiredParameters,
          optionalParameters,
          title));
  }

  private static ANScenarioListBuilder postArrivalNotices(
    ScenarioType scenarioType, String title, boolean validateProducerPayload) {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
      previousAction ->
        new PublisherPostANAction(
          publisherPartyName,
          subscriberPartyName,
          (ANAction) previousAction,
          scenarioType,
          componentFactory.getMessageSchemaValidator("PostArrivalNoticesRequest"),
          title,
          validateProducerPayload));
  }

  private static ANScenarioListBuilder getArrivalNotices(
    String title, boolean expectNextPageCursor) {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
      previousAction ->
        new SubscriberGetANAction(
          subscriberPartyName,
          publisherPartyName,
          (ANAction) previousAction,
          componentFactory.getMessageSchemaValidator("GetArrivalNoticesResponse"),
          title,
          expectNextPageCursor));
  }

  private static ANScenarioListBuilder postArrivalNoticesNotification(String title) {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
      previousAction ->
        new PublisherPostANNotificationAction(
          publisherPartyName,
          subscriberPartyName,
          (ANAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            "PostArrivalNoticeNotificationsRequest"),
          title));
  }

  private static ANScenarioListBuilder noAction() {
    return new ANScenarioListBuilder(null);
  }

  private static ANScenarioListBuilder withScenarioTitlePrefix(
    ANScenarioListBuilder builder, String prefix) {
    builder.withScenarioTitlePrefix(prefix);
    return builder;
  }
}
