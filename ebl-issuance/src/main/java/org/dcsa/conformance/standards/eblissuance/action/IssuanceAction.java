package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonAttributeCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblissuance.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.eblissuance.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;
import org.dcsa.conformance.standards.eblissuance.party.SuppliedScenarioParameters;

public abstract class IssuanceAction extends ConformanceAction {
  private final OverwritingReference<DynamicScenarioParameters> dspReference;
  protected final int expectedStatus;

  protected IssuanceAction(
      String sourcePartyName,
      String targetPartyName,
      IssuanceAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    if (previousAction == null) {
      this.dspReference =
          new OverwritingReference<>(null, new DynamicScenarioParameters(EblType.STRAIGHT_EBL));
    } else {
      this.dspReference = new OverwritingReference<>(previousAction.dspReference, null);
    }
  }

  protected IssuanceAction getPreviousIssuanceAction() {
    return (IssuanceAction) previousAction;
  }


  public DynamicScenarioParameters getDsp() {
    return this.dspReference.get();
  }

  public void setDsp(DynamicScenarioParameters dsp) {
    this.dspReference.set(dsp);
  }

  @Override
  public ObjectNode exportJsonState() {
    var state = super.exportJsonState();
    if (dspReference.hasCurrentValue()) {
      state.set("dsp", getDsp().toJson());
    }
    return state;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (jsonState.has("dsp")) {
      this.setDsp(DynamicScenarioParameters.fromJson(jsonState.path("dsp")));
    }
  }

  protected Consumer<SuppliedScenarioParameters> getSspConsumer() {
    return getPreviousIssuanceAction().getSspConsumer();
  }

  protected Supplier<SuppliedScenarioParameters> getSspSupplier() {
    return getPreviousIssuanceAction().getSspSupplier();
  }

  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return getPreviousIssuanceAction().getCspConsumer();
  }

  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return getPreviousIssuanceAction().getCspSupplier();
  }


  protected abstract Supplier<String> getTdrSupplier();

  protected Stream<ActionCheck> getNotificationChecks(
      String expectedApiVersion, JsonSchemaValidator notificationSchemaValidator) {
    String titlePrefix = "[Notification]";

    return Stream.of(
            new HttpMethodCheck(
                titlePrefix,
                EblIssuanceRole::isPlatform,
                getMatchedNotificationExchangeUuid(),
                "POST"),
            new UrlPathCheck(
                titlePrefix,
                EblIssuanceRole::isPlatform,
                getMatchedNotificationExchangeUuid(),
                "/v3/ebl-issuance-responses"),
            new ResponseStatusCheck(
                titlePrefix, EblIssuanceRole::isCarrier, getMatchedNotificationExchangeUuid(), 204),
            new JsonSchemaCheck(
                titlePrefix,
                EblIssuanceRole::isPlatform,
                getMatchedNotificationExchangeUuid(),
                HttpMessageType.REQUEST,
                notificationSchemaValidator))
        .filter(Objects::nonNull);
  }
}
