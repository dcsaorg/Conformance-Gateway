package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

@Getter
@Slf4j
public class SurrenderRequestResponseErrorAction extends ConformanceAction {

  private final Supplier<JsonNode> inputBodySupplier;
  private final JsonSchemaValidator responseSchemaValidator;

  public SurrenderRequestResponseErrorAction(
      String platformPartyName,
      String carrierPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        platformPartyName,
        carrierPartyName,
        previousAction,
        "Surrender (Error)");
    this.responseSchemaValidator = responseSchemaValidator;
    this.inputBodySupplier = findInputBodySupplier(previousAction);
  }

  @Override
  public String getHumanReadablePrompt() {
    return EblSurrenderAction.getMarkdownHumanReadablePrompt(
        Map.of(), "prompt-surrender-reqres-error.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("inputBody", inputBodySupplier.get());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(EblSurrenderRole::isPlatform, getMatchedExchangeUuid(), "POST"),
            new ApiHeaderCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblSurrenderRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblSurrenderRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }

  private static Supplier<JsonNode> findInputBodySupplier(ConformanceAction action) {
    return action instanceof SupplyScenarioParametersErrorAction errorSupplyAction
      ? errorSupplyAction::getInputBody
      : findInputBodySupplier(action.getPreviousAction());
  }
}
