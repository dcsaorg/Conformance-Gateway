package org.dcsa.conformance.standards.vgm;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.vgm.action.ConsumerGetVgmDeclarationAction;
import org.dcsa.conformance.standards.vgm.action.ProducerPostVgmDeclarationAction;
import org.dcsa.conformance.standards.vgm.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.vgm.action.VgmAction;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class VgmScenarioListBuilder extends ScenarioListBuilder<VgmScenarioListBuilder> {

  private static final ThreadLocal<VgmComponentFactory> threadLocalComponentFactory = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  private VgmScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, VgmScenarioListBuilder> createModuleScenarioListBuilders(
    VgmComponentFactory anComponentFactory, Set<String> testedPartyRoleNames, String producerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(anComponentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    Map<String, Map<String, VgmScenarioListBuilder>> partyScenariosMap = MapUtils.orderedMap(
      Map.entry(
        VgmRole.PRODUCER.getConfigName(),
        MapUtils.orderedMap(
          Map.entry("POST scenario - Alternative required path for VGM push", postVgmDeclaration()),
          Map.entry(
            "GET scenarios for required query parameter filters - Alternative required path for VGM pull",
            noAction()
              .thenEither(
                supplyScenarioParameters(VgmQueryParameters.CBR).then(getVgmDeclaration()),
                supplyScenarioParameters(VgmQueryParameters.CBR, VgmQueryParameters.ER).then(getVgmDeclaration()),
                supplyScenarioParameters(VgmQueryParameters.TDR).then(getVgmDeclaration()),
                supplyScenarioParameters(VgmQueryParameters.TDR, VgmQueryParameters.ER).then(getVgmDeclaration()),
                supplyScenarioParameters(VgmQueryParameters.ER).then(getVgmDeclaration()),
                supplyScenarioParameters(VgmQueryParameters.CBR, VgmQueryParameters.ER, VgmQueryParameters.DDT_MIN, VgmQueryParameters.DDT_MAX).then(getVgmDeclaration()))),
          Map.entry(
            "GET scenario for optional query parameters (pagination) — Optional/report-only",
            supplyScenarioParameters(VgmQueryParameters.CBR, VgmQueryParameters.LIMIT)
              .then(getVgmDeclaration(true).then(getVgmDeclaration()))
              .asOptionalReportOnlyScenario()))),
      Map.entry(
        VgmRole.CONSUMER.getConfigName(),
        MapUtils.orderedMap(
          Map.entry("POST scenario - Alternative required path for VGM push", postVgmDeclaration()),
          Map.entry("GET scenario - Alternative required path for VGM pull", getVgmDeclaration()))));

    if (testedPartyRoleNames.size() > 1) {
      testedPartyRoleNames.forEach(role ->
        partyScenariosMap.getOrDefault(role, Map.of()).values()
          .forEach(builder -> builder.withScenarioTitlePrefix(role + ": ")));
    }
    return MapUtils.mergePartyScenarioModules(partyScenariosMap, testedPartyRoleNames);
  }

  private static VgmScenarioListBuilder supplyScenarioParameters(
    VgmQueryParameters... queryParameters) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new VgmScenarioListBuilder(
      previousAction -> new SupplyScenarioParametersAction(producerPartyName, queryParameters));
  }

  private static VgmScenarioListBuilder getVgmDeclaration() {
    return getVgmDeclaration(false);
  }

  private static VgmScenarioListBuilder getVgmDeclaration(boolean hasNextPage) {
    VgmComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new VgmScenarioListBuilder(
      previousAction ->
        new ConsumerGetVgmDeclarationAction(
          consumerPartyName,
          producerPartyName,
          (VgmAction) previousAction,
          hasNextPage,
          componentFactory.getMessageSchemaValidator("GetVGMDeclarationsResponse")));
  }

  private static VgmScenarioListBuilder postVgmDeclaration() {
    VgmComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new VgmScenarioListBuilder(
      previousAction ->
        new ProducerPostVgmDeclarationAction(
          producerPartyName,
          consumerPartyName,
          (VgmAction) previousAction,
          componentFactory.getMessageSchemaValidator("PostVGMDeclarationsRequest")));
  }

  private static VgmScenarioListBuilder noAction() {
    return new VgmScenarioListBuilder(null);
  }
}
