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
  /**
   * The value of the `shippingInstructionsStatus` to be returned in the next GET request.
   */
  private final ShippingInstructionsStatus shippingInstructionsStatus;
  /**
   * The value of the `updatedShippingInstructionsStatus` to be returned in the next GET request.
   */
  private final ShippingInstructionsStatus updatedShippingInstructionsStatus;
  /**
   * This is a pseudo status used when the status is reset via the "cancel update" request.
   *
   * <p>The cancel request
   */
  private final ShippingInstructionsStatus memorizedShippingInstructionsStatus;
  /**
   * This is a pseudo status used by the RI generator to determine where to go next.
   */
  @NonNull
  private final ShippingInstructionsStatus stateGeneratorShippingInstructionsStatus;
  @NonNull
  private final TransportDocumentStatus currentTransportDocumentStatus;
  private final boolean isAlsoRequestingAmendedSI;
  private final boolean usingTDRRefInGetDefault;
  @Getter
  private final boolean areUnhappyPathsAvailable;

  private static EblScenarioState INITIAL_STATE = new EblScenarioState(
    null,
    null,
    null,
    ScenarioType.REGULAR_SWB,
    null,
    null,
    null,
    ShippingInstructionsStatus.SI_START,
    TransportDocumentStatus.TD_START,
    false,
    false,
    true
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
