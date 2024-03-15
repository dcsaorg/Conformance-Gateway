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

@Slf4j
public class EblIssuanceScenarioListBuilder
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
    if (componentFactory.getStandardVersion().startsWith("2.")) {
      // Niels N. and Tom is still debating whether we should invest anymore effort into V2.
      return Stream.of(
          Map.entry(
            "",
            supplyScenarioParameters(EblType.STRAIGHT_EBL)
              .thenEither(
                correctIssuanceRequest()
                  .thenEither(
                    issuanceResponseAccepted()
                      .thenEither(
                        noAction(),
                        duplicateIssuanceRequest(),
                        duplicateIssuanceResponse()),
                    duplicateIssuanceRequest().then(issuanceResponseAccepted()),
                    issuanceResponseBlocked()
                      .then(
                        correctIssuanceRequest().then(issuanceResponseAccepted())),
                    issuanceResponseRefused()
                      .then(
                        correctIssuanceRequest().then(issuanceResponseAccepted()))),
                incorrectIssuanceRequest()
                  .then(correctIssuanceRequest().then(issuanceResponseAccepted())))))
        .collect(
          Collectors.toMap(
            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
    return Stream.of(
            Map.entry(
                "",
                noAction().thenEither(
                  supplyScenarioParameters(EblType.STRAIGHT_EBL)
                      .thenEither(
                          correctIssuanceRequest()
                              .thenEither(
                                  issuanceResponseAccepted()
                                      .thenEither(
                                          noAction(),
                                          duplicateIssuanceRequest(),
                                          duplicateIssuanceResponse()),
                                  duplicateIssuanceRequest().then(issuanceResponseAccepted()),
                                  issuanceResponseBlocked()
                                      .then(
                                          correctIssuanceRequest().then(issuanceResponseAccepted())),
                                  issuanceResponseRefused()
                                      .then(
                                          correctIssuanceRequest().then(issuanceResponseAccepted()))),
                          incorrectIssuanceRequest()
                              .then(correctIssuanceRequest().then(issuanceResponseAccepted()))),
                supplyScenarioParameters(EblType.NEGOTIABLE_EBL).then(
                          correctIssuanceRequest().then(issuanceResponseAccepted())),
                supplyScenarioParameters(EblType.BLANK_EBL).then(
                          correctIssuanceRequest().then(issuanceResponseAccepted()))))
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

  private static EblIssuanceScenarioListBuilder supplyScenarioParameters(EblType eblType) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(platformPartyName, carrierPartyName, null, eblType));
  }

  private static EblIssuanceScenarioListBuilder correctIssuanceRequest() {
    return _issuanceRequest(true, false);
  }

  private static EblIssuanceScenarioListBuilder correctIssuanceRequest(EblType eblType) {
    return _issuanceRequest(true, false);
  }


  private static EblIssuanceScenarioListBuilder incorrectIssuanceRequest() {
    return _issuanceRequest(false, false);
  }

  private static EblIssuanceScenarioListBuilder duplicateIssuanceRequest() {
    return _issuanceRequest(true, true);
  }

  private static EblIssuanceScenarioListBuilder _issuanceRequest(
      boolean isCorrect,
      boolean isDuplicate
  ) {
    EblIssuanceComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
        previousAction ->
            new IssuanceRequestAction(
                isCorrect,
                isDuplicate,
                platformPartyName,
                carrierPartyName,
                (IssuanceAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblIssuanceRole.CARRIER.getConfigName(), true)));
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseAccepted() {
    return _issuanceResponse(IssuanceResponseCode.ACCEPTED, false);
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseBlocked() {
    return _issuanceResponse(IssuanceResponseCode.BLOCKED, false);
  }

  private static EblIssuanceScenarioListBuilder issuanceResponseRefused() {
    return _issuanceResponse(IssuanceResponseCode.REFUSED, false);
  }

  private static EblIssuanceScenarioListBuilder duplicateIssuanceResponse() {
    return _issuanceResponse(IssuanceResponseCode.ACCEPTED, true);
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
                    EblIssuanceRole.PLATFORM.getConfigName(), true)));
  }
}
