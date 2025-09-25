package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.*;

public class Shipper_GetTransportDocumentAction extends EblAction {

  private final TransportDocumentStatus expectedTdStatus;
  private final JsonSchemaValidator responseSchemaValidator;

  public Shipper_GetTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      TransportDocumentStatus expectedTdStatus,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET TD", 200);
    this.expectedTdStatus = expectedTdStatus;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("tdr", getDspSupplier().get().transportDocumentReference());
  }


  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()),
        "prompt-shipper-get-td.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    // SD-1997 gradually wiping out from production orchestrator states the big docs that should not have been added to the DSP
    dsp = dsp.withTransportDocument(null).withPreviousTransportDocument(null);
    getDspConsumer().accept(dsp);
  }

  @Override
  public Set<String> skippableForRoles() {
    return Set.of(EblRole.SHIPPER.getConfigName());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    var dsp = getDspSupplier().get();
    var tdr = dsp.transportDocumentReference() != null ? dsp.transportDocumentReference() : "<UNKNOWN-TDR>";
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EblRole::isShipper, getMatchedExchangeUuid(), "/v3/transport-documents/" + tdr),
            new ResponseStatusCheck(EblRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            // FIXME SD-1997 implement this properly, fetching the exchange by the matched UUID of
            // an earlier action
            // checkTDChanged(getMatchedExchangeUuid(), expectedApiVersion, dsp), // see commit
            // history
            EblChecks.tdPlusScenarioContentChecks(
                getMatchedExchangeUuid(), expectedApiVersion, expectedTdStatus, getDspSupplier()));
      }
    };
  }
}
