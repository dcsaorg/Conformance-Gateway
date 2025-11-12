package org.dcsa.conformance.standards.vgm.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.vgm.checks.VgmChecks;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

public class ConsumerGetVgmDeclarationAction extends VgmAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public ConsumerGetVgmDeclarationAction(
      String sourcePartyName,
      String targetPartyName,
      VgmAction previousAction,
      JsonSchemaValidator schemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "GET VGM Declaration");
    this.responseSchemaValidator = schemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(VgmRole::isConsumer, getMatchedExchangeUuid(), "/vgm-declarations"),
            new ResponseStatusCheck(VgmRole::isProducer, getMatchedExchangeUuid(), 200),
            new JsonSchemaCheck(
                VgmRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                VgmRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                VgmRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            VgmChecks.getVGMGetPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }
}
