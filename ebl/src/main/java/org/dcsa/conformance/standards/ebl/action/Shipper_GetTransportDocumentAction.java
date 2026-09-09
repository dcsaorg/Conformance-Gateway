package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.*;

public class Shipper_GetTransportDocumentAction extends EblAction {

  private final List<TransportDocumentStatus> expectedTdStatus;
  private final JsonSchemaValidator responseSchemaValidator;

  public Shipper_GetTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      List<TransportDocumentStatus> expectedTdStatus,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET TD", 200, true);
    this.expectedTdStatus = expectedTdStatus;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", getTransportDocumentReference());
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getTransportDocumentReference()),
        "prompt-shipper-get-td.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    getDspConsumer().accept(dsp);
  }


  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "GET"),
            new UrlPathCheck(
                EblRole::isShipper, getMatchedExchangeUuid(), getTransportDocumentEndpoint()),
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
            EblChecks.tdPlusScenarioContentChecks(
                getMatchedExchangeUuid(), expectedApiVersion, expectedTdStatus, getDspSupplier()));
      }
    };
  }
}
