package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblissuance.checks.IssuanceChecks;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@Getter
@Slf4j
public class IssuanceRequestResponseAction extends IssuanceAction {
  private final IssuanceResponseCode issuanceResponseCode;
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator issuanceManifestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final AtomicReference<String> transportDocumentReference;

  public IssuanceRequestResponseAction(
      String platformPartyName,
      String carrierPartyName,
      IssuanceAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator issuanceManifestSchemaValidator) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "Request() Response(%s)"
            .formatted(
                _latestPlatformScenarioParametersAction(previousAction)
                    .getResponseCode()
                    .standardCode),
        204);
    this.issuanceResponseCode =
        _latestPlatformScenarioParametersAction(previousAction).getResponseCode();
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.requestSchemaValidator = requestSchemaValidator;
    this.issuanceManifestSchemaValidator = issuanceManifestSchemaValidator;
    this.transportDocumentReference =
        previousAction != null && !(this.previousAction instanceof PlatformScenarioParametersAction)
            ? null
            : new AtomicReference<>();
  }

  private static PlatformScenarioParametersAction _latestPlatformScenarioParametersAction(
      IssuanceAction previousAction) {
    return previousAction
            instanceof PlatformScenarioParametersAction platformScenarioParametersAction
        ? platformScenarioParametersAction
        : _latestPlatformScenarioParametersAction(previousAction.getPreviousIssuanceAction());
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return this.previousAction != null
            && !(this.previousAction instanceof PlatformScenarioParametersAction)
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
    return ("Send an issuance request for %s")
        .formatted(
            tdr == null
                ? "an eBL of type %s that has not yet been issued".formatted(eblType)
                : "the eBL with transportDocumentReference '%s'".formatted(tdr));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
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
        Supplier<SignatureVerifier> signatureVerifier =
            () ->
                PayloadSignerFactory.verifierFromPemEncodedPublicKey(
                    getCspSupplier().get().carrierSigningKeyPEM());
        String asyncResponseChecksPrefix = "[Response]";
        UUID matchedExchangeUuid = getMatchedExchangeUuid();
        UUID matchedNotificationExchangeUuid = getMatchedNotificationExchangeUuid();
        return Stream.concat(
            Stream.of(
                new UrlPathCheck(
                    EblIssuanceRole::isCarrier, matchedExchangeUuid, "/ebl-issuance-requests"),
                new HttpMethodCheck(EblIssuanceRole::isCarrier, matchedExchangeUuid, "PUT"),
                new ResponseStatusCheck(
                    EblIssuanceRole::isPlatform, matchedExchangeUuid, expectedStatus),
                new ApiHeaderCheck(
                    EblIssuanceRole::isCarrier,
                    matchedExchangeUuid,
                    HttpMessageType.REQUEST,
                    expectedApiVersion),
                new ApiHeaderCheck(
                    EblIssuanceRole::isPlatform,
                    matchedExchangeUuid,
                    HttpMessageType.RESPONSE,
                    expectedApiVersion),
                new JsonSchemaCheck(
                    EblIssuanceRole::isCarrier,
                    matchedExchangeUuid,
                    HttpMessageType.REQUEST,
                    requestSchemaValidator),
                IssuanceChecks.tdScenarioChecks(
                    matchedExchangeUuid, expectedApiVersion, getDsp().eblType()),
                IssuanceChecks.issuanceRequestSignatureChecks(
                    matchedExchangeUuid,
                    expectedApiVersion,
                    issuanceManifestSchemaValidator,
                    signatureVerifier),
                IssuanceChecks.tdContentChecks(matchedExchangeUuid, expectedApiVersion)),
            Stream.of(
                new HttpMethodCheck(
                    asyncResponseChecksPrefix,
                    EblIssuanceRole::isPlatform,
                    matchedNotificationExchangeUuid,
                    "POST"),
                new UrlPathCheck(
                    asyncResponseChecksPrefix,
                    EblIssuanceRole::isPlatform,
                    matchedNotificationExchangeUuid,
                    "/v3/ebl-issuance-responses"),
                new ResponseStatusCheck(
                    asyncResponseChecksPrefix,
                    EblIssuanceRole::isCarrier,
                    matchedNotificationExchangeUuid,
                    204),
                new JsonSchemaCheck(
                    asyncResponseChecksPrefix,
                    EblIssuanceRole::isPlatform,
                    matchedNotificationExchangeUuid,
                    HttpMessageType.REQUEST,
                    notificationSchemaValidator),
                new JsonAttributeCheck(
                    asyncResponseChecksPrefix,
                    EblIssuanceRole::isPlatform,
                    matchedNotificationExchangeUuid,
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/issuanceResponseCode"),
                    issuanceResponseCode.standardCode)));
      }
    };
  }
}
