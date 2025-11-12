package org.dcsa.conformance.standards.vgm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.vgm.action.ConsumerGetVgmDeclarationAction;
import org.dcsa.conformance.standards.vgm.action.ProducerPostVgmDeclarationAction;
import org.dcsa.conformance.standards.vgm.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.vgm.action.VgmAction;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;

public class VgmScenarioListBuilder extends ScenarioListBuilder<VgmScenarioListBuilder> {

  private static final ThreadLocal<VgmComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  private VgmScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, VgmScenarioListBuilder> createModuleScenarioListBuilders(
      VgmComponentFactory anComponentFactory, String producerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(anComponentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    return Stream.of(
            Map.entry("POST scenarios", postVgmDeclaration()),
            Map.entry(
                "GET scenarios for all required query parameter filters",
                noAction()
                    .thenEither(
                        supplyScenarioParameters(VgmQueryParameters.CBR).then(getVgmDeclaration()),
                        supplyScenarioParameters(VgmQueryParameters.CBR, VgmQueryParameters.ER)
                            .then(getVgmDeclaration()),
                        supplyScenarioParameters(VgmQueryParameters.TDR).then(getVgmDeclaration()),
                        supplyScenarioParameters(VgmQueryParameters.TDR, VgmQueryParameters.CBR)
                            .then(getVgmDeclaration()),
                        supplyScenarioParameters(VgmQueryParameters.ER).then(getVgmDeclaration()))),
            Map.entry(
                "GET scenario for any optional query parameter filters",
                supplyScenarioParameters().then(getVgmDeclaration())))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static VgmScenarioListBuilder supplyScenarioParameters(
      VgmQueryParameters... queryParameters) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new VgmScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(producerPartyName, queryParameters));
  }

  private static VgmScenarioListBuilder getVgmDeclaration() {
    VgmComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new VgmScenarioListBuilder(
        previousAction ->
            new ConsumerGetVgmDeclarationAction(
                consumerPartyName,
                producerPartyName,
                (VgmAction) previousAction,
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
