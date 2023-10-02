package org.dcsa.conformance.standards.eblsurrender.v10.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Role;

@Getter
public class SurrenderResponseAction extends TdrAction {
  private final boolean accept;
  private Supplier<String> srrSupplier;

  public SurrenderResponseAction(
      boolean accept,
      String carrierPartyName,
      String platformPartyName,
      int expectedStatus,
      ConformanceAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(accept ? "SURR" : "SREJ", expectedStatus));
    this.accept = accept;
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

  private static final String SCHEMAS_FOLDER = "/eblsurrender/v10/";
  private static final String SCHEMAS_FILE_ASYNC_RESPONSE =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-response.json";
  private static final JsonSchemaValidator requestJsonSchemaValidator =
      new JsonSchemaValidator(SCHEMAS_FILE_ASYNC_RESPONSE, "surrenderRequestAnswer");

  @Override
  public ConformanceCheck createCheck() {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        String expectedSrr = getSrrSupplier().get();
        return Stream.of(
                new UrlPathCheck(
                    EblSurrenderV10Role::isCarrier,
                    getMatchedExchangeUuid(),
                    "/v1/surrender-request-responses"),
                new ResponseStatusCheck(
                    EblSurrenderV10Role::isPlatform, getMatchedExchangeUuid(), getExpectedStatus()),
                new ApiHeaderCheck(
                    EblSurrenderV10Role::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    "1.0.0"),
                new ApiHeaderCheck(
                    EblSurrenderV10Role::isPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    "1.0.0"),
                new JsonSchemaCheck(
                    EblSurrenderV10Role::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    requestJsonSchemaValidator),
                new JsonAttributeCheck(
                    EblSurrenderV10Role::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    "action",
                    accept ? "SURR" : "SREJ"),
                expectedSrr.equals("*")
                    ? null
                    : new JsonAttributeCheck(
                        EblSurrenderV10Role::isCarrier,
                        getMatchedExchangeUuid(),
                        HttpMessageType.REQUEST,
                        "surrenderRequestReference",
                        expectedSrr))
            .filter(Objects::nonNull);
      }
    };
  }
}
