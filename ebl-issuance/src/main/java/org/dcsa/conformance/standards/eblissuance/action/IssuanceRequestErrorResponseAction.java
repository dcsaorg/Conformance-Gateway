package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
@Slf4j
public class IssuanceRequestErrorResponseAction extends IssuanceAction {

  public static final String SEND_NO_ISSUING_PARTY = "sendNoIssuingParty";

  private final AtomicReference<String> transportDocumentReference;
  private final JsonSchemaValidator responseSchemaValidator;

  public IssuanceRequestErrorResponseAction(
      String platformPartyName,
      String carrierPartyName,
      IssuanceAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "Request() Response(%s) (no issuing party)"
            .formatted(
                latestPlatformScenarioParametersAction(previousAction)
                    .getResponseCode()
                    .standardCode),
            400);
    this.responseSchemaValidator = responseSchemaValidator;
    this.transportDocumentReference =
        previousAction != null && !(this.previousAction instanceof PlatformScenarioParametersAction)
            ? null
            : new AtomicReference<>();
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return this.previousAction != null
            && !(this.previousAction instanceof PlatformScenarioParametersAction)
        ? ((IssuanceAction) this.previousAction).getTdrSupplier()
        : this.transportDocumentReference::get;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(Map.of(), "prompt-iss-reqres-error.md");
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
    jsonNode.put(SEND_NO_ISSUING_PARTY, true);
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(EblIssuanceRole::isCarrier, getMatchedExchangeUuid(), "PUT"),
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
            new JsonSchemaCheck(
                EblIssuanceRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }
}
