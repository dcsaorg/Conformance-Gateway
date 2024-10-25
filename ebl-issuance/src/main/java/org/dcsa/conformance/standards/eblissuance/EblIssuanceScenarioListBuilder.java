package org.dcsa.conformance.standards.eblissuance;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblissuance.action.*;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

import static org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode.ACCEPTED;

@Slf4j
class EblIssuanceScenarioListBuilder
    extends ScenarioListBuilder<EblIssuanceScenarioListBuilder> {
  private static final ThreadLocal<EblIssuanceComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, EblIssuanceScenarioListBuilder>
      createModuleScenarioListBuilders(
          EblIssuanceComponentFactory componentFactory,
          String carrierPartyName,
          String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);
    return Stream.of(
      Map.entry(
        "eBL types",
        carrierScenarioParameters().then(
          noAction().thenEither(
            supplyScenarioParameters(EblType.STRAIGHT_EBL,
              IssuanceResponseCode.ACCEPTED)
              .then(correctIssuanceRequestResponse()),
            supplyScenarioParameters(EblType.NEGOTIABLE_EBL,IssuanceResponseCode.ACCEPTED).then(
              correctIssuanceRequestResponse()),
            supplyScenarioParameters(EblType.BLANK_EBL,IssuanceResponseCode.ACCEPTED).then(
              correctIssuanceRequestResponse()))))
      /*Map.entry("Error cases",
        carrierScenarioParameters().then(noAction().thenEither(
          supplyScenarioParameters(EblType.STRAIGHT_EBL).then(duplicateIssuanceRequest().then(issuanceResponseBlocked())),
          supplyScenarioParameters(EblType.STRAIGHT_EBL).then(duplicateIssuanceRequest().then(issuanceResponseRefused()))
        ))
      )*/
    ).collect(
      Collectors.toMap(
        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }


  private EblIssuanceScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblIssuanceScenarioListBuilder noAction() {
    return new EblIssuanceScenarioListBuilder(null);
  }

  private static EblIssuanceScenarioListBuilder supplyScenarioParameters(EblType eblType, IssuanceResponseCode responseCode) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(platformPartyName, carrierPartyName, (IssuanceAction) previousAction, eblType,responseCode));
  }

  private static EblIssuanceScenarioListBuilder carrierScenarioParameters() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new CarrierScenarioParametersAction(
                carrierPartyName, platformPartyName, (IssuanceAction) previousAction));
  }

  private static EblIssuanceScenarioListBuilder correctIssuanceRequestResponse() {
    return _issuanceRequestResponse(true, false, false, ACCEPTED);
  }

  private static EblIssuanceScenarioListBuilder _issuanceRequestResponse(
      boolean isCorrect,
      boolean isDuplicate,
      boolean isAmended,
      IssuanceResponseCode code
  ) {
    EblIssuanceComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new IssuanceRequestResponseAction(
                isCorrect,
                isDuplicate,
                isAmended,
                platformPartyName,
                carrierPartyName,
                (IssuanceAction) previousAction,
              code,
              componentFactory.getMessageSchemaValidator(
                EblIssuanceRole.PLATFORM.getConfigName(), true, false),
                componentFactory.getMessageSchemaValidator(
                    EblIssuanceRole.CARRIER.getConfigName(), true, false),
                componentFactory.getMessageSchemaValidator(
                  EblIssuanceRole.CARRIER.getConfigName(), true, true)));
  }

}
