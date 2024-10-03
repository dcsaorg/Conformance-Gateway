package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@Getter
public class IssuanceResponseAction extends IssuanceAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final IssuanceResponseCode issuanceResponseCode;

  public IssuanceResponseAction(
      IssuanceResponseCode issuanceResponseCode,
      boolean isDuplicate,
      String carrierPartyName,
      String platformPartyName,
      IssuanceAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(
        platformPartyName,
        carrierPartyName,
        previousAction,
        "Response(%s%s)"
            .formatted(issuanceResponseCode.standardCode, isDuplicate ? ",duplicate" : ""),
        isDuplicate ? 409 : 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.issuanceResponseCode = issuanceResponseCode;
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return ((IssuanceAction) this.previousAction).getTdrSupplier();
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Respond with '%s' to the issuance request for transport document reference '%s'")
        .formatted(this.issuanceResponseCode.standardCode, getTdrSupplier().get());
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("irc", this.issuanceResponseCode.standardCode);
    String tdr = getTdrSupplier().get();
    if (tdr != null) {
      jsonNode.put("tdr", tdr);
    }
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EblIssuanceRole::isPlatform, getMatchedExchangeUuid(), "/ebl-issuance-responses"),
            new HttpMethodCheck(EblIssuanceRole::isPlatform, getMatchedExchangeUuid(), "POST"),
            new ResponseStatusCheck(
                EblIssuanceRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                EblIssuanceRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblIssuanceRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblIssuanceRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator));
      }
    };
  }
}
