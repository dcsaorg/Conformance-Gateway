package org.dcsa.conformance.end;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.end.action.ConsumerGetEndorsementChainAction;
import org.dcsa.conformance.end.action.EndorsementChainAction;
import org.dcsa.conformance.end.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE;
import static org.dcsa.conformance.end.party.EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_SUB_REFERENCE;
import static org.dcsa.conformance.end.party.EndorsementChainRole.CONSUMER;
import static org.dcsa.conformance.end.party.EndorsementChainRole.PROVIDER;

public class EndorsementChainScenarioListBuilder
  extends ScenarioListBuilder<EndorsementChainScenarioListBuilder> {
  protected EndorsementChainScenarioListBuilder(
    Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static final ThreadLocal<EndorsementChainComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProviderPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  public static Map<String, EndorsementChainScenarioListBuilder> createModuleScenarioListBuilders(
    EndorsementChainComponentFactory endComponentChainFactory,
    Set<String> testedPartyRoleNames,
    String providerPartyName,
    String consumerPartyName) {

    threadLocalComponentFactory.set(endComponentChainFactory);
    threadLocalProviderPartyName.set(providerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    Map<String, Map<String, EndorsementChainScenarioListBuilder>> modulesByRole =
      MapUtils.orderedMap(
        Map.entry(PROVIDER.getConfigName(), providerScenarios()),
        Map.entry(CONSUMER.getConfigName(), consumerScenarios()));

    return MapUtils.mergePartyScenarioModules(modulesByRole, testedPartyRoleNames);
  }

  private static Map<String, EndorsementChainScenarioListBuilder> providerScenarios() {
    Map<String, EndorsementChainScenarioListBuilder> providerScenarios = new LinkedHashMap<>();
    providerScenarios.put(
      "Provider Conformance Scenarios",
      noAction()
        .thenEither(
          providerScenario("TDR", TRANSPORT_DOCUMENT_REFERENCE),
          providerScenario(
            "TDR + TDSR",
            TRANSPORT_DOCUMENT_REFERENCE,
            TRANSPORT_DOCUMENT_SUB_REFERENCE))
        .asInterchangeableScenarios());
    return providerScenarios;
  }

  private static Map<String, EndorsementChainScenarioListBuilder> consumerScenarios() {
    Map<String, EndorsementChainScenarioListBuilder> consumerScenarios = new LinkedHashMap<>();
    consumerScenarios.put(
      "Consumer Conformance Scenarios",
      noAction()
        .thenEither(
          consumerScenario("TDR", TRANSPORT_DOCUMENT_REFERENCE),
          consumerScenario(
            "TDR + TDSR",
            TRANSPORT_DOCUMENT_REFERENCE,
            TRANSPORT_DOCUMENT_SUB_REFERENCE))
        .asInterchangeableScenarios());
    return consumerScenarios;
  }

  private static EndorsementChainScenarioListBuilder noAction() {
    return new EndorsementChainScenarioListBuilder(null);
  }

  private static EndorsementChainScenarioListBuilder providerScenario(
    String parameterLabel, EndorsementChainFilterParameter... filterParameters) {
    return supplyScenarioParameters("SupplyCSP[%s]".formatted(parameterLabel), filterParameters)
      .then(getEndorsementChain("GET EndorsementChain", null));
  }

  private static EndorsementChainScenarioListBuilder consumerScenario(
    String parameterLabel, EndorsementChainFilterParameter... filterParameters) {
    Map<EndorsementChainFilterParameter, String> values = new LinkedHashMap<>();
    for (EndorsementChainFilterParameter filterParameter : filterParameters) {
      values.put(
        filterParameter,
        switch (filterParameter) {
          case TRANSPORT_DOCUMENT_REFERENCE -> "HHL71800000";
          case TRANSPORT_DOCUMENT_SUB_REFERENCE -> "fc5009a7-25ad-4bb0-9892-4e2dea6bcdd9";
          case CARRIER_SCAC_CODE -> "YMLU";
        });
    }
    return getEndorsementChain(
      "GET EndorsementChain (%s)".formatted(parameterLabel),
      SuppliedScenarioParameters.fromMap(values));
  }

  private static EndorsementChainScenarioListBuilder supplyScenarioParameters(
    String actionTitle, EndorsementChainFilterParameter... filterParameters) {
    String providerPartyName = threadLocalProviderPartyName.get();
    return new EndorsementChainScenarioListBuilder(
      _ ->
        new SupplyScenarioParametersAction(providerPartyName, actionTitle, filterParameters));
  }

  private static EndorsementChainScenarioListBuilder getEndorsementChain(
    String actionTitle, SuppliedScenarioParameters standaloneScenarioParameters) {
    EndorsementChainComponentFactory componentFactory = threadLocalComponentFactory.get();
    String providerPartyName = threadLocalProviderPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new EndorsementChainScenarioListBuilder(
      previousAction ->
        new ConsumerGetEndorsementChainAction(
          providerPartyName,
          consumerPartyName,
          (EndorsementChainAction) previousAction,
          componentFactory.getMessageSchemaValidator("endorsementChains"),
          actionTitle,
          standaloneScenarioParameters));
  }
}
