package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblissuance.checks.IssuanceChecks;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@Getter
@Slf4j
public class IssuanceRequestAction extends IssuanceAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean isCorrect;

  private final AtomicReference<String> transportDocumentReference;

  public IssuanceRequestAction(
      boolean isCorrect,
      boolean isDuplicate,
      String platformPartyName,
      String carrierPartyName,
      IssuanceAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "Request(%s%s%s)"
            .formatted(
                isCorrect ? "" : "incorrect",
                !isCorrect && isDuplicate ? "," : "",
                isDuplicate ? "duplicate" : ""),
        isCorrect ? isDuplicate ? 409 : 204 : 400);
    this.isCorrect = isCorrect;
    this.requestSchemaValidator = requestSchemaValidator;
    this.transportDocumentReference =
        previousAction != null && !(this.previousAction instanceof SupplyScenarioParametersAction)
            ? null
            : new AtomicReference<>();
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return this.previousAction != null
            && !(this.previousAction instanceof SupplyScenarioParametersAction)
        ? ((IssuanceAction) this.previousAction).getTdrSupplier()
        : this.transportDocumentReference::get;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (transportDocumentReference != null) {
      String tdr = transportDocumentReference.get();
      if (tdr != null) {
        jsonState.put("transportDocumentReference", tdr);
      }
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (transportDocumentReference != null) {
      JsonNode tdrNode = jsonState.get("transportDocumentReference");
      if (tdrNode != null) {
        transportDocumentReference.set(tdrNode.asText());
      }
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    String tdr = getTdrSupplier().get();
    return ("Send %s issuance request for %s")
        .formatted(
            isCorrect ? "an" : "an incorrect",
            tdr == null
                ? "an eBL that has not yet been issued"
                : "the eBL with transportDocumentReference '%s'".formatted(tdr));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("isCorrect", isCorrect);
    jsonNode.set("ssp", getSspSupplier().get().toJson());
    String tdr = getTdrSupplier().get();
    if (tdr != null) {
      jsonNode.put("tdr", tdr);
    }
    return jsonNode;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String exchangeTdr = requestJsonNode.get("document").get("transportDocumentReference").asText();
    if (transportDocumentReference != null && transportDocumentReference.get() == null) {
      transportDocumentReference.set(exchangeTdr);
    } else {
      String expectedTdr = getTdrSupplier().get();
      if (!Objects.equals(exchangeTdr, expectedTdr)) {
        throw new IllegalStateException(
            "Exchange TDR '%s' does not match expected TDR '%s'"
                .formatted(exchangeTdr, expectedTdr));
      }
    }
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
                new UrlPathCheck(
                    EblIssuanceRole::isCarrier, getMatchedExchangeUuid(), "/ebl-issuance-requests"),
                new ResponseStatusCheck(
                    EblIssuanceRole::isPlatform, getMatchedExchangeUuid(), expectedStatus),
                new ApiHeaderCheck(
                    EblIssuanceRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    expectedApiVersion),
                new ApiHeaderCheck(
                    EblIssuanceRole::isPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    expectedApiVersion),
                isCorrect
                    ? new JsonSchemaCheck(
                        EblIssuanceRole::isCarrier,
                        getMatchedExchangeUuid(),
                        HttpMessageType.REQUEST,
                        requestSchemaValidator)
                    : null,
                isCorrect && expectedApiVersion.startsWith("3.")
                  ? IssuanceChecks.tdContentChecks(getMatchedExchangeUuid())
                  : null
            )
            .filter(Objects::nonNull);
      }
    };
  }
}
