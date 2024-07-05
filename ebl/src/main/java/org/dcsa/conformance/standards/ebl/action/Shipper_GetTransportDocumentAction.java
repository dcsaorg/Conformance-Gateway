package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
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
    return "GET the TD with reference '%s'"
        .formatted(getDspSupplier().get().transportDocumentReference());
  }

  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    var response = exchange.getResponse().message().body().getJsonBody();
    var previousTD = dsp.transportDocument();
    dsp = dsp.withPreviousTransportDocument(previousTD)
      .withTransportDocument(response);
    getDspConsumer().accept(dsp);
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
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                "/v3/transport-documents/" + tdr),
            new ResponseStatusCheck(
                EblRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
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
            checkTDChanged(getMatchedExchangeUuid(), expectedApiVersion, dsp),
            EBLChecks.tdPlusScenarioContentChecks(getMatchedExchangeUuid(), expectedApiVersion, expectedTdStatus, getCspSupplier(), getDspSupplier()));
      }
    };
  }

  private static ActionCheck checkTDChanged(UUID matched, String standardsVersion, DynamicScenarioParameters dsp) {
    var deltaCheck = JsonAttribute.lostAttributeCheck(
      "(ignored)",
      dsp::previousTransportDocument,
      (baselineTD, currentTD) -> {
        if (baselineTD instanceof ObjectNode td) {
          td.remove("transportDocumentStatus");
        }
    });
    JsonContentMatchedValidation hadChangesCheck = (nodeToValidate, contextPath) -> {
      var currentStatus = nodeToValidate.path("transportDocumentStatus").asText("");
      var comparisonTD = dsp.previousTransportDocument();
      var comparisonStatus = comparisonTD.path("transportDocumentStatus").asText("");
      if (dsp.newTransportDocumentContent()) {
        return Set.of();
      }
      if (!(nodeToValidate instanceof ObjectNode currentTDObj) || !(comparisonTD instanceof ObjectNode comparisonTDObj)) {
        // Schema validation takes care of this.
        return Set.of();
      }

      var currentTDObjCopy = currentTDObj.deepCopy();
      var comparisonTDObjCopy = comparisonTDObj.deepCopy();
      currentTDObjCopy.remove("transportDocumentStatus");
      comparisonTDObjCopy.remove("transportDocumentStatus");
      var checksum = Checksums.sha256CanonicalJson(currentTDObjCopy);
      var previousChecksum = Checksums.sha256CanonicalJson(comparisonTDObjCopy);
      if (checksum.equals(previousChecksum)) {
        return Set.of("Expected a change, but it is the same TD. " + currentStatus + " - " + comparisonStatus);
      }
      return Set.of();
    };
    return JsonAttribute.contentChecks(
        "",
        "[Scenario] Validate TD changes match the expected",
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        standardsVersion,
        JsonAttribute.customValidator("The TD match the scenario step",
          JsonAttribute.ifMatchedThenElse(
            // For some cases, we assume the TD will change in ways we cannot predict, so here
            // we just effectively skip the check
            //
            // Common cases are new drafts and amendments.
            (ignored) -> dsp.newTransportDocumentContent(),
            hadChangesCheck,
            deltaCheck::validate
          )
        )
      );
  }
}
