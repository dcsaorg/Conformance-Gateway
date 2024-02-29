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
    return noAction()
        .thenEither(
          scenarioWithParameters(CARRIER_SERVICE_CODE),
          scenarioWithParameters(CARRIER_SERVICE_CODE, LIMIT),
          scenarioWithParameters(UNIVERSAL_SERVICE_REFERENCE),
          scenarioWithParameters(UNIVERSAL_SERVICE_REFERENCE, LIMIT),
          scenarioWithParameters(VESSEL_IMO_NUMBER),
          scenarioWithParameters(VESSEL_IMO_NUMBER, LIMIT),
          scenarioWithParameters(UN_LOCATION_CODE),
          scenarioWithParameters(UN_LOCATION_CODE, LIMIT),
          scenarioWithParameters(UN_LOCATION_CODE, FACILITY_SMDG_CODE),
          scenarioWithParameters(UN_LOCATION_CODE, FACILITY_SMDG_CODE, LIMIT),
          scenarioWithParameters(CARRIER_VOYAGE_NUMBER, CARRIER_SERVICE_CODE),
          scenarioWithParameters(CARRIER_VOYAGE_NUMBER, CARRIER_SERVICE_CODE, LIMIT),
          scenarioWithParameters(CARRIER_VOYAGE_NUMBER, UNIVERSAL_SERVICE_REFERENCE),
          scenarioWithParameters(CARRIER_VOYAGE_NUMBER, UNIVERSAL_SERVICE_REFERENCE, LIMIT),
          scenarioWithParameters(UNIVERSAL_VOYAGE_REFERENCE, CARRIER_SERVICE_CODE),
          scenarioWithParameters(UNIVERSAL_VOYAGE_REFERENCE, CARRIER_SERVICE_CODE, LIMIT),
          scenarioWithParameters(UNIVERSAL_VOYAGE_REFERENCE, UNIVERSAL_SERVICE_REFERENCE),
          scenarioWithParameters(
              UNIVERSAL_VOYAGE_REFERENCE, UNIVERSAL_SERVICE_REFERENCE, LIMIT));
  }

  private OvsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static OvsScenarioListBuilder noAction() {
    return new OvsScenarioListBuilder(null);
  }

  private static OvsScenarioListBuilder scenarioWithParameters(
    OvsFilterParameter... ovsFilterParameters) {
    return supplyScenarioParameters(ovsFilterParameters).then(getSchedules());
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
