package org.dcsa.conformance.standards.adoption.action;

import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.adoption.party.AdoptionRole;

import java.util.stream.Stream;

public class GetAdoptionStatsAction extends ConformanceAction {

  public static final String GET_ADOPTION_STATS_URL = "/v1/adoption-stats";

  private final JsonSchemaValidator schemaValidator;

  public GetAdoptionStatsAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      JsonSchemaValidator schemaValidator) {
    super(sourcePartyName, targetPartyName, previousAction, "GetAdoptionStats");
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send GET request to get adoption statistics";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
          new UrlPathCheck(AdoptionRole::isDCSA, getMatchedExchangeUuid(), GET_ADOPTION_STATS_URL),
          new ResponseStatusCheck(AdoptionRole::isAdopter, getMatchedExchangeUuid(), 200),
          new JsonSchemaCheck(
            AdoptionRole::isAdopter,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            schemaValidator),
          new ApiHeaderCheck(
            AdoptionRole::isAdopter,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            expectedApiVersion),
          new ApiHeaderCheck(
            AdoptionRole::isDCSA,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            expectedApiVersion));
      }
    };
  }
}
