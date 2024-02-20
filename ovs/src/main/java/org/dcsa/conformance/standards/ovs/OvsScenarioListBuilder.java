package org.dcsa.conformance.standards.ovs;

import static org.dcsa.conformance.standards.ovs.party.OvsFilterParameter.*;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ovs.action.OvsGetSchedulesAction;
import org.dcsa.conformance.standards.ovs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.ovs.party.OvsFilterParameter;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Slf4j
public class OvsScenarioListBuilder extends ScenarioListBuilder<OvsScenarioListBuilder> {
  private static final ThreadLocal<OvsComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static OvsScenarioListBuilder buildTree(
      OvsComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    if ("3.0.0-Beta1".equals(componentFactory.getStandardVersion())) {
      return noAction().then(getSchedules());
    } else {
      return noAction()
          .thenEither(
              scenarioWithFilterBy(CARRIER_SERVICE_NAME),
              scenarioWithFilterByDatesAnd(CARRIER_SERVICE_NAME),
              scenarioWithFilterBy(CARRIER_SERVICE_CODE),
              scenarioWithFilterByDatesAnd(CARRIER_SERVICE_CODE),
              scenarioWithFilterBy(UNIVERSAL_SERVICE_REFERENCE),
              scenarioWithFilterByDatesAnd(UNIVERSAL_SERVICE_REFERENCE),
              scenarioWithFilterBy(VESSEL_IMO_NUMBER),
              scenarioWithFilterByDatesAnd(VESSEL_IMO_NUMBER),
              scenarioWithFilterBy(VESSEL_NAME),
              scenarioWithFilterByDatesAnd(VESSEL_NAME),
              scenarioWithFilterBy(CARRIER_VOYAGE_NUMBER),
              scenarioWithFilterByDatesAnd(CARRIER_VOYAGE_NUMBER),
              scenarioWithFilterBy(UNIVERSAL_VOYAGE_REFERENCE),
              scenarioWithFilterByDatesAnd(UNIVERSAL_VOYAGE_REFERENCE),
              scenarioWithFilterBy(UN_LOCATION_CODE),
              scenarioWithFilterByDatesAnd(UN_LOCATION_CODE),
              scenarioWithFilterBy(FACILITY_SMDG_CODE),
              scenarioWithFilterByDatesAnd(FACILITY_SMDG_CODE),
              scenarioWithFilterBy(START_DATE),
              scenarioWithFilterBy(END_DATE),
              scenarioWithFilterBy(START_DATE, END_DATE));
    }
  }

  private OvsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static OvsScenarioListBuilder noAction() {
    return new OvsScenarioListBuilder(null);
  }

  private static OvsScenarioListBuilder scenarioWithFilterBy(OvsFilterParameter parameter1) {
    return supplyScenarioParameters(LIMIT, parameter1).then(getSchedules());
  }

  private static OvsScenarioListBuilder scenarioWithFilterByDatesAnd(
      OvsFilterParameter parameter1) {
    return supplyScenarioParameters(LIMIT, START_DATE, END_DATE, parameter1).then(getSchedules());
  }

  private static OvsScenarioListBuilder scenarioWithFilterBy(
      OvsFilterParameter parameter1, OvsFilterParameter parameter2) {
    return supplyScenarioParameters(LIMIT, parameter1, parameter2).then(getSchedules());
  }

  private static OvsScenarioListBuilder supplyScenarioParameters(
      OvsFilterParameter... ovsFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, ovsFilterParameters));
  }

  private static OvsScenarioListBuilder getSchedules() {
    OvsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction ->
            new OvsGetSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    OvsRole.PUBLISHER.getConfigName(), false)));
  }
}
