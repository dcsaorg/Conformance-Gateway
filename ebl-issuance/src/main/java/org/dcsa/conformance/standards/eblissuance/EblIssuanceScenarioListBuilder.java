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
              .then(correctIssuanceRequestResponse())/*,
            supplyScenarioParameters(EblType.NEGOTIABLE_EBL).then(
              correctIssuanceRequestResponse()),
            supplyScenarioParameters(EblType.BLANK_EBL).then(
              correctIssuanceRequestResponse())*/)))
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

 /* private static EblIssuanceScenarioListBuilder correctAmendedIssuanceRequest() {
    return _issuanceRequest(true, false, true);
  }

  private static EblIssuanceScenarioListBuilder duplicateAmendedIssuanceRequest() {
    return _issuanceRequest(true, true, true);
  }

  private static EblIssuanceScenarioListBuilder incorrectIssuanceRequest() {
    return _issuanceRequest(false, false, false);
  }*/

  /*private static EblIssuanceScenarioListBuilder duplicateIssuanceRequest() {
    return _issuanceRequestResponse(true, true, false);
  }*/

  private static EblIssuanceScenarioListBuilder _issuanceRequestResponse(
      boolean isCorrect,
      boolean isDuplicate,
      boolean isAmended,
      IssuanceResponseCode responseCode
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
              responseCode,
                (IssuanceAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblIssuanceRole.CARRIER.getConfigName(), true, false),
                componentFactory.getMessageSchemaValidator(
                  EblIssuanceRole.CARRIER.getConfigName(), true, true)));
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseAccepted() {
    return _issuanceResponse(ACCEPTED, false);
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseBlocked() {
    return _issuanceResponse(IssuanceResponseCode.BLOCKED, false);
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseRefused() {
    return _issuanceResponse(IssuanceResponseCode.REFUSED, false);
  }

  private static EblIssuanceScenarioListBuilder duplicateIssuanceResponse() {
    return _issuanceResponse(ACCEPTED, true);
  }

  private static EblIssuanceScenarioListBuilder _issuanceResponse(
      IssuanceResponseCode issuanceResponseCode, boolean isDuplicate) {
    EblIssuanceComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new IssuanceResponseAction(
                issuanceResponseCode,
                isDuplicate,
                carrierPartyName,
                platformPartyName,
                (IssuanceAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblIssuanceRole.PLATFORM.getConfigName(), true, false)));
  }
}
