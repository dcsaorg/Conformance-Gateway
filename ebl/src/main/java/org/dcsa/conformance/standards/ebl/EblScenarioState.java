package org.dcsa.conformance.standards.ebl;

import java.util.function.Function;
import lombok.*;
import org.dcsa.conformance.standards.ebl.action.EblAction;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.ebl.sb.ScenarioManager;
import org.dcsa.conformance.standards.ebl.sb.ScenarioSingleStateStepHandler;
import org.dcsa.conformance.standards.ebl.sb.ScenarioState;
import org.dcsa.conformance.standards.ebl.sb.ScenarioStepHandler;

@Getter
@With
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class EblScenarioState implements SIScenarioState<EblAction, EblScenarioState>, TDScenarioState<EblAction, EblScenarioState> {

  private final ScenarioState<EblAction, EblScenarioState> previousStepState;
  private final Function<EblAction, EblAction> conformanceActionGenerator;
  private final ScenarioManager scenarioManager;
  @NonNull
  private final ScenarioType scenarioType;
  @NonNull
  private final ShippingInstructionsStatus shippingInstructionsStatus;
  private final ShippingInstructionsStatus updatedShippingInstructionsStatus;
  private final ShippingInstructionsStatus memorizedShippingInstructionsStatus;
  @NonNull
  private final TransportDocumentStatus currentTransportDocumentStatus;
  private final boolean usingTDRRefInGetDefault;
  private final boolean usingHappyPathOnly;

  private static EblScenarioState INITIAL_STATE = new EblScenarioState(
    null,
    null,
    null,
    ScenarioType.REGULAR_SWB,
    ShippingInstructionsStatus.SI_START,
    null,
    null,
    TransportDocumentStatus.TD_START,
    false,
    false
  );

  private static EblScenarioState initialState(ScenarioManager sm) {
    return INITIAL_STATE.toBuilder().scenarioManager(sm).build();
  }

  public static ScenarioSingleStateStepHandler<EblAction, EblScenarioState> initialScenarioState(ScenarioManager sm) {
    return ScenarioStepHandler.fromInitialState(initialState(sm));
  }

  public static ScenarioSingleStateStepHandler<EblAction, EblScenarioState> initialScenarioState(ScenarioManager sm, Function<EblScenarioState, EblScenarioState> initializer) {
    return ScenarioStepHandler.fromInitialState(initializer.apply(initialState(sm)));
  }

  @Override
  public EblScenarioState finishScenario() {
    var sm = scenarioManager;
    if (sm == null) {
      throw new IllegalStateException("Scenario is already finished");
    }
    sm.addScenario(this);
    return this.toBuilder()
      .scenarioManager(null)
      .build();
  }

  @Override
  public boolean isFinished() {
    return scenarioManager == null;
  }

}
