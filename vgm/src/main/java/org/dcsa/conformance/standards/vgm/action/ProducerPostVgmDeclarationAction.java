package org.dcsa.conformance.standards.vgm.action;

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

public class ProducerPostVgmDeclarationAction extends VgmAction {

  private final JsonSchemaValidator requestSchemaValidator;

  public ProducerPostVgmDeclarationAction(
      String sourcePartyName,
      String targetPartyName,
      VgmAction previousAction,
      JsonSchemaValidator schemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "POST VGM Declaration");
    this.requestSchemaValidator = schemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a POST request to the sandbox endpoint '/vgm-declarations' with one or more VGM declarations in the request body. The VGM declarations must comply with the VGM API specification and demonstrate the correct use of required objects and attributes as defined in the conformance checks.";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(VgmRole::isProducer, getMatchedExchangeUuid(), "/vgm-declarations"),
            new ResponseStatusCheck(VgmRole::isConsumer, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                VgmRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new ApiHeaderCheck(
                VgmRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new JsonSchemaCheck(
                VgmRole::isProducer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            VgmChecks.getVGMPostPayloadChecks(getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }
}
