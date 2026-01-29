package org.dcsa.conformance.end;

import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.CARRIER_SCAC_CODE;
import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_SUB_REFERENCE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.end.action.CarrierGetEndorsementChainAction;
import org.dcsa.conformance.end.action.EndorsementChainAction;
import org.dcsa.conformance.end.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;

public class EndorsementChainScenarioListBuilder
    extends ScenarioListBuilder<EndorsementChainScenarioListBuilder> {
  protected EndorsementChainScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static final ThreadLocal<EndorsementChainComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static Map<String, EndorsementChainScenarioListBuilder> createModuleScenarioListBuilders(
      EndorsementChainComponentFactory endComponentChainFactory,
      String providerPartyName,
      String carrierPartyName) {

    threadLocalComponentFactory.set(endComponentChainFactory);
    threadLocalPublisherPartyName.set(providerPartyName);
    threadLocalSubscriberPartyName.set(carrierPartyName);

    Map<String, EndorsementChainScenarioListBuilder> map = new LinkedHashMap<>();

    map.put(
        "GET-only scenarios (for adopters only supporting GET)",
        noAction()
            .thenEither(
                scenarioWithParameters(),
                scenarioWithParameters(TRANSPORT_DOCUMENT_SUB_REFERENCE),
                scenarioWithParameters(CARRIER_SCAC_CODE),
                scenarioWithParameters(
                    TRANSPORT_DOCUMENT_SUB_REFERENCE,
                    CARRIER_SCAC_CODE)));
    return map;
  }

  private static EndorsementChainScenarioListBuilder noAction() {
    return new EndorsementChainScenarioListBuilder(null);
  }

  private static EndorsementChainScenarioListBuilder scenarioWithParameters(
      EndorsementChainFilterParameter... endorsementChainFilterParameter) {
    return supplyScenarioParameters(endorsementChainFilterParameter).then(getEndorsementChain());
  }

  private static EndorsementChainScenarioListBuilder supplyScenarioParameters(
      EndorsementChainFilterParameter... endorsementChainFilterParameter) {
    String providerPartyName = threadLocalPublisherPartyName.get();
    return new EndorsementChainScenarioListBuilder(
        _ ->
            new SupplyScenarioParametersAction(providerPartyName, endorsementChainFilterParameter));
  }

  private static EndorsementChainScenarioListBuilder getEndorsementChain() {
    EndorsementChainComponentFactory componentFactory = threadLocalComponentFactory.get();
    String providerPartyName = threadLocalPublisherPartyName.get();
    String carrierPartyName = threadLocalSubscriberPartyName.get();
    return new EndorsementChainScenarioListBuilder(
        previousAction ->
            new CarrierGetEndorsementChainAction(
                providerPartyName,
                carrierPartyName,
                (EndorsementChainAction) previousAction,
                componentFactory.getMessageSchemaValidator("endorsementChains")));
  }
}
