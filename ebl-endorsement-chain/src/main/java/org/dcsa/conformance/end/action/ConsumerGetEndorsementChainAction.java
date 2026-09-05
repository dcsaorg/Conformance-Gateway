package org.dcsa.conformance.end.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.end.checks.EndorsementChainChecks;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;
import org.dcsa.conformance.end.party.EndorsementChainRole;
import org.dcsa.conformance.end.party.SuppliedScenarioParameters;

import java.util.Map;
import java.util.stream.Stream;

public class ConsumerGetEndorsementChainAction extends EndorsementChainAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public ConsumerGetEndorsementChainAction(
    String providerPartyName,
    String consumerPartyName,
    EndorsementChainAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    String actionTitle,
    SuppliedScenarioParameters standaloneScenarioParameters) {
    super(
      consumerPartyName,
      providerPartyName,
      previousAction,
      actionTitle,
      standaloneScenarioParameters);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    JsonNode root = sspSupplier.get().toJson();

    String tdr = root.get("transportDocumentReference").asText();

    ObjectNode queryParams = root.deepCopy();
    queryParams.remove("transportDocumentReference"); // remove path param field

    return queryParams.isEmpty()
      ? getMarkdownHumanReadablePrompt(Map.of("TDR", tdr), "prompt-consumer-get.md")
      : getMarkdownHumanReadablePrompt(
      Map.of("TDR", tdr, "QUERY_PARAMS", queryParams.toString()),
      "prompt-consumer-get-with-queryparams.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        String tdr =
          sspSupplier
            .get()
            .getMap()
            .getOrDefault(EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE, "");
        return
          Stream.of(
            new UrlPathCheck(
              EndorsementChainRole::isConsumer,
              getMatchedExchangeUuid(),
              "/endorsement-chains/" + tdr),
            ResponseStatusCheck.forSuccessfulResponse(
              EndorsementChainRole::isProvider, getMatchedExchangeUuid()),
            new JsonSchemaCheck(
              EndorsementChainRole::isProvider,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              responseSchemaValidator),
            new ApiHeaderCheck(
              EndorsementChainRole::isConsumer,
              getMatchedExchangeUuid(),
              HttpMessageType.REQUEST,
              expectedApiVersion),
            new ApiHeaderCheck(
              EndorsementChainRole::isProvider,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              expectedApiVersion),
            EndorsementChainChecks.getENDGetResponseChecks(
              getMatchedExchangeUuid(), expectedApiVersion));
      }
    };
  }


  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}


