package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
@Slf4j
public class SurrenderRequestResponseCarrierErrorAction extends EblSurrenderAction {

  private static final int RESPONSE_CODE = 409;

  private final AtomicReference<String> surrenderRequestReference = new AtomicReference<>();
  private final Supplier<String> srrSupplier = surrenderRequestReference::get;
  private final JsonSchemaValidator responseSchemaValidator;

  public SurrenderRequestResponseCarrierErrorAction(
      String platformPartyName,
      String carrierPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        platformPartyName,
        carrierPartyName,
        RESPONSE_CODE,
        previousAction,
        "SurrenderForDelivery (not available for surrender)");
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    return srrSupplier;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(Map.of(), "prompt-surrender-reqres-error.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("forAmendment", false);
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(EblSurrenderRole::isCarrier, getMatchedExchangeUuid(), "POST"),
            new ResponseStatusCheck(
                EblSurrenderRole::isPlatform, getMatchedExchangeUuid(), RESPONSE_CODE),
            new ApiHeaderCheck(
                EblSurrenderRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }
}
