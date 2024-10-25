package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblissuance.checks.IssuanceChecks;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@Getter
@Slf4j
public class IssuanceRequestResponseAction extends IssuanceAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator issuanceManifestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean isCorrect;
  private final boolean isAmended;
  private String responseCode;

  private final AtomicReference<String> transportDocumentReference;

  private static String stepNameArg(
    boolean isCorrect,
    boolean isDuplicate,
    boolean isAmended
  ) {
    return Stream.of(
      isCorrect ? "correct" : "incorrect",
      isDuplicate ? "duplicate" : null,
      isAmended ? "amended" : null
    ).filter(Objects::nonNull)
    .collect(Collectors.joining(","));
  }

  public IssuanceRequestResponseAction(
      boolean isCorrect,
      boolean isDuplicate,
      boolean isAmended,
      String platformPartyName,
      String carrierPartyName,
      IssuanceAction previousAction,
      IssuanceResponseCode code,
      JsonSchemaValidator notificationSchemaValidator,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator issuanceManifestSchemaValidator) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "Request(%s)Response(%s)"
            .formatted(stepNameArg(isCorrect, isDuplicate, isAmended),code.standardCode),
        isCorrect ? isDuplicate ? 409 : 204 : 400);
    this.isCorrect = isCorrect;
    this.isAmended = isAmended;
    this.responseCode = code.standardCode;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.requestSchemaValidator = requestSchemaValidator;
    this.issuanceManifestSchemaValidator = issuanceManifestSchemaValidator;
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
    var eblType = getDsp().eblType();
    return ("Send %s issuance request for %s")
        .formatted(
            isCorrect ? "an" : "an incorrect",
            tdr == null
                ? "an eBL that has not yet been issued of type %s".formatted(eblType)
                : "%s the eBL with transportDocumentReference '%s'".formatted(
                  tdr,
                  isAmended ?  "an amended version of" : ""
            ));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("isCorrect", isCorrect)
      .put("isAmended", isAmended);
    jsonNode.set("dsp", getDsp().toJson());
    jsonNode.set("ssp", getSspSupplier().get().toJson());
    jsonNode.set("csp", getCspSupplier().get().toJson());
    String tdr = getTdrSupplier().get();
    if (tdr != null) {
      jsonNode.put("tdr", tdr);
    }
    return jsonNode;
  }
@Override
  protected boolean expectsNotificationExchange() {
    return true;
  }
  @Override
  public boolean isMissingMatchedExchange() {
    return super.isMissingMatchedExchange();
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
        Supplier<SignatureVerifier> signatureVerifier = () -> PayloadSignerFactory.verifierFromPemEncodedPublicKey(getCspSupplier().get().carrierSigningKeyPEM());
        Stream<ActionCheck> primaryExchangeChecks = Stream.of(
                new UrlPathCheck(
                    EblIssuanceRole::isCarrier, getMatchedExchangeUuid(), "/ebl-issuance-requests"),
                new HttpMethodCheck(EblIssuanceRole::isCarrier,getMatchedExchangeUuid(),"PUT"),
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
                expectedApiVersion.startsWith("3.0")
                  ? IssuanceChecks.tdScenarioChecks(getMatchedExchangeUuid(), expectedApiVersion, getDsp().eblType())
                  : null,
                isCorrect && expectedApiVersion.startsWith("3.")
                  ? IssuanceChecks.issuanceRequestSignatureChecks(getMatchedExchangeUuid(), expectedApiVersion, issuanceManifestSchemaValidator, signatureVerifier)
                  : null,
                isCorrect && expectedApiVersion.startsWith("3.")
                  ? IssuanceChecks.tdContentChecks(getMatchedExchangeUuid(), expectedApiVersion)
                  : null
            ).filter(Objects::nonNull);
        return Stream.concat(
          primaryExchangeChecks,
          getNotificationChecks(notificationSchemaValidator));
      }
    };
  }
}
