package org.dcsa.conformance.standards.eblissuance;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@Slf4j
public class EblIssuanceScenarioListBuilder
    extends ScenarioListBuilder<EblIssuanceScenarioListBuilder> {
  private static final ThreadLocal<EblIssuanceComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  public static EblIssuanceScenarioListBuilder buildTree(
      EblIssuanceComponentFactory componentFactory,
      String carrierPartyName,
      String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);
    return noAction()
        .thenEither(
            correctIssuanceRequest()
                .thenEither(
                    issuanceResponseAccepted()
                        .thenEither(
                            noAction(), duplicateIssuanceRequest(), duplicateIssuanceResponse()),
                    duplicateIssuanceRequest().then(issuanceResponseAccepted()),
                    issuanceResponseBlocked()
                        .then(correctIssuanceRequest().then(issuanceResponseAccepted())),
                    issuanceResponseRefused()
                        .then(correctIssuanceRequest().then(issuanceResponseAccepted()))),
            incorrectIssuanceRequest()
                .then(correctIssuanceRequest().then(issuanceResponseAccepted())));
  }

  private EblIssuanceScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblIssuanceScenarioListBuilder noAction() {
    return new EblIssuanceScenarioListBuilder(null);
  }

  private static EblIssuanceScenarioListBuilder correctIssuanceRequest() {
    return _issuanceRequest(true, false);
  }

  private static EblIssuanceScenarioListBuilder incorrectIssuanceRequest() {
    return _issuanceRequest(false, false);
  }

  private static EblIssuanceScenarioListBuilder duplicateIssuanceRequest() {
    return _issuanceRequest(true, true);
  }

  private static EblIssuanceScenarioListBuilder _issuanceRequest(
      boolean isCorrect, boolean isDuplicate) {
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
