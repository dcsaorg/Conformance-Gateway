package org.dcsa.conformance.end.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.QueryParamCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.end.checks.EndorsementChainChecks;
import org.dcsa.conformance.end.party.EndorsementChainFilterParameter;
import org.dcsa.conformance.end.party.EndorsementChainRole;

public class CarrierGetEndorsementChainAction extends EndorsementChainAction{

  private final JsonSchemaValidator responseSchemaValidator;

  public CarrierGetEndorsementChainAction(
      String providerPartyName,
      String carrierPartyName,
      EndorsementChainAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(carrierPartyName, providerPartyName, previousAction, "GetEndorsementChain");
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    JsonNode root = sspSupplier.get().toJson();

    String tdr = root.get("transportDocumentReference").asText();

    ObjectNode queryParams = root.deepCopy();
    queryParams.remove("transportDocumentReference"); // remove path param field

    return queryParams.isEmpty()
        ? getMarkdownHumanReadablePrompt(Map.of("TDR", tdr), "prompt-carrier-get.md")
        : getMarkdownHumanReadablePrompt(
            Map.of("TDR", tdr, "QUERY_PARAMS", queryParams.toString()),
            "prompt-carrier-get-with-queryparams.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var tdr = dsp.transportDocumentReference() != null ? dsp.transportDocumentReference() : "";
        var checks =
            Stream.of(
                new UrlPathCheck(
                    EndorsementChainRole::isCarrier,
                    getMatchedExchangeUuid(),
                    "/endorsement-chains/" + tdr),
                new ResponseStatusCheck(
                    EndorsementChainRole::isProvider, getMatchedExchangeUuid(), 200),
                new JsonSchemaCheck(
                    EndorsementChainRole::isProvider,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    responseSchemaValidator),
                new ApiHeaderCheck(
                    EndorsementChainRole::isCarrier,
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

        // Build query-param checks from SSP (excluding the path param)
        var queryParamChecks =
            sspSupplier.get() == null
                ? Stream.<QueryParamCheck>empty()
                : sspSupplier.get().getMap().entrySet().stream()
                    .filter(
                        e ->
                            e.getKey()
                                != EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE)
                    .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                    .map(
                        e ->
                            new QueryParamCheck(
                                EndorsementChainRole::isCarrier,
                                getMatchedExchangeUuid(),
                                e.getKey().getParamName(),
                                e.getValue()));

        return Stream.concat(checks, queryParamChecks);
      }
    };
      }



  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonActionNode =
        super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    return jsonActionNode;
  }
}
