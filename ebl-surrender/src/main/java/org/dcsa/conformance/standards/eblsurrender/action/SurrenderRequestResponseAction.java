package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.Map;
import java.util.stream.Stream;

import static org.dcsa.conformance.standards.eblsurrender.checks.SurrenderChecks.surrenderRequestChecks;
import static org.dcsa.conformance.standards.eblsurrender.checks.SurrenderChecks.surrenderResponseChecks;

public class SurrenderRequestResponseAction extends EblSurrenderAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean forAmendment;

  public SurrenderRequestResponseAction(
    boolean forAmendment,
    String platformPartyName,
    String carrierPartyName,
    int expectedStatus,
    ConformanceAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator) {
    super(
      platformPartyName,
      carrierPartyName,
      expectedStatus,
      previousAction,
      forAmendment
        ? "Surrender request (amendment) & asynchronous response"
        : "Surrender request (delivery) & asynchronous response");
    this.forAmendment = forAmendment;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      Map.of("SURRENDER_TYPE", forAmendment ? "amendment" : "delivery"),
      "prompt-surrender-reqres.md");
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("forAmendment", forAmendment);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.concat(
          Stream.of(
            new UrlPathCheck(
              EblSurrenderRole::isPlatform,
              getMatchedExchangeUuid(),
              "/ebl-surrender-requests"),
            new ResponseStatusCheck(

              EblSurrenderRole::isCarrier, getMatchedExchangeUuid(), getExpectedStatus()),
            new ApiHeaderCheck(
              EblSurrenderRole::isPlatform,
              getMatchedExchangeUuid(),
              HttpMessageType.REQUEST,
              expectedApiVersion),
            new ApiHeaderCheck(
              EblSurrenderRole::isCarrier,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              expectedApiVersion),
            new JsonSchemaCheck(
              EblSurrenderRole::isPlatform,
              getMatchedExchangeUuid(),
              HttpMessageType.REQUEST,
              requestSchemaValidator),
            surrenderRequestChecks(getMatchedExchangeUuid(), expectedApiVersion, forAmendment ? "AREQ" : "SREQ")),
          Stream.of(
            new UrlPathCheck(
              "[Response]",
              EblSurrenderRole::isCarrier,
              getMatchedNotificationExchangeUuid(),
              "/ebl-surrender-responses"),
            new ResponseStatusCheck(
              "[Response]",
              EblSurrenderRole::isPlatform,
              getMatchedNotificationExchangeUuid(),
              getExpectedStatus()),
            new ApiHeaderCheck(
              EblSurrenderRole::isPlatform,
              getMatchedNotificationExchangeUuid(),
              HttpMessageType.RESPONSE,
              expectedApiVersion),
            new JsonSchemaCheck(
              "[Response]",
              EblSurrenderRole::isCarrier,
              getMatchedNotificationExchangeUuid(),
              HttpMessageType.REQUEST,
              responseSchemaValidator),
            surrenderResponseChecks(getMatchedNotificationExchangeUuid(), expectedApiVersion)));
      }
    };
  }
}
