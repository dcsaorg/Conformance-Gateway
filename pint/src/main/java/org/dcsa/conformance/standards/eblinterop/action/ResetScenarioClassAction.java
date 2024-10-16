package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ResetScenarioClassAction extends PintAction {
  private final ScenarioClass scenarioClass;

  public ResetScenarioClassAction(
      String receivingPlatform,
      String sendingPlatform,
      PintAction previousAction,
      ScenarioClass scenarioClass) {
    super(
        receivingPlatform,
        sendingPlatform,
        previousAction,
        "ResetScenarioClass(%s)"
            .formatted(scenarioClass.name()),
      -1
      );
    this.scenarioClass = scenarioClass;
    if (!scenarioClass.canResetToClass()) {
      throw new IllegalArgumentException("%s cannot be used with %s".formatted(scenarioClass.name(), this.getScenarioClass().name()));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("scenarioClass", this.scenarioClass.name())
      .put("transportDocumentReference", this.getSsp().transportDocumentReference());
  }


  @Override
  public String getHumanReadablePrompt() {
    return ("Reset the scenario handling to %s.".formatted(this.scenarioClass.name()));
  }
}
