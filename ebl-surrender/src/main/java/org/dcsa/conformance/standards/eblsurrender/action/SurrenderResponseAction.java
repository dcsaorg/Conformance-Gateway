package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

@Getter
public class SurrenderResponseAction extends TdrAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean accept;
  private Supplier<String> srrSupplier;

  public SurrenderResponseAction(
      boolean accept,
      String carrierPartyName,
      String platformPartyName,
      int expectedStatus,
      ConformanceAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(
        carrierPartyName,
        platformPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(accept ? "SURR" : "SREJ", expectedStatus));
    this.accept = accept;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public ObjectNode exportJsonState() {
    return super.exportJsonState();
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("%s the surrender request with surrender request reference '%s'")
        .formatted(accept ? "Accept" : "Reject", getSrrSupplier().get());
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    if (srrSupplier != null) return srrSupplier;
    for (ConformanceAction action = this.previousAction;
        action != null;
        action = action.getPreviousAction()) {
      if (action instanceof TdrAction tdrAction) {
        if ((srrSupplier = tdrAction.getSrrSupplier()) != null) {
          return srrSupplier;
        }
      }
    }
    return () -> "*";
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("srr", getSrrSupplier().get()).put("accept", accept);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        String expectedSrr = getSrrSupplier().get();
        return Stream.of(
                new UrlPathCheck(
                    EblSurrenderRole::isCarrier,
                    getMatchedExchangeUuid(),
                    "/ebl-surrender-responses"),
                new ResponseStatusCheck(
                    EblSurrenderRole::isPlatform, getMatchedExchangeUuid(), getExpectedStatus()),
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
                    EblSurrenderRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    requestSchemaValidator),
                new JsonAttributeCheck(
                    EblSurrenderRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/action"),
                    accept ? "SURR" : "SREJ"),
                expectedSrr == null || expectedSrr.equals("*")
                    ? null
                    : new JsonAttributeCheck(
                        EblSurrenderRole::isCarrier,
                        getMatchedExchangeUuid(),
                        HttpMessageType.REQUEST,
                        JsonPointer.compile("/surrenderRequestReference"),
                        expectedSrr))
            .filter(Objects::nonNull);
      }
    };
  }
}
