package org.dcsa.conformance.standards.adoption.action;

import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.adoption.party.AdoptionRole;

public class PutAdoptionStatsAction extends ConformanceAction {

  public static final String PUT_ADOPTION_STATS_URL = "/v1/adoption-stats";

  private final JsonSchemaValidator schemaValidator;

  public PutAdoptionStatsAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator schemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "PutAdoptionStats");
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send PUT request with adoption stats";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                AdoptionRole::isAdopter, getMatchedExchangeUuid(), PUT_ADOPTION_STATS_URL),
            new ResponseStatusCheck(AdoptionRole::isDCSA, getMatchedExchangeUuid(), 204),
            new JsonSchemaCheck(
                AdoptionRole::isAdopter,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                schemaValidator),
            new ApiHeaderCheck(
                AdoptionRole::isAdopter,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                AdoptionRole::isDCSA,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion));
      }
    };
  }
}
