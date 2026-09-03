package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheckRebaser;
import org.dcsa.conformance.core.check.JsonRebasableContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public class Shipper_GetTransportDocumentAmendmentAction extends EblAction {
  private static final Set<TransportDocumentStatus> PRIMARY_STATUSES =
      Set.of(
          TransportDocumentStatus.TD_DRAFT,
          TransportDocumentStatus.TD_ISSUED,
          TransportDocumentStatus.TD_PENDING_SURRENDER_FOR_AMENDMENT);

  private final JsonSchemaValidator responseSchemaValidator;
  private final AmendedTransportDocumentStatus expectedAmendmentStatus;

  public Shipper_GetTransportDocumentAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator responseSchemaValidator,
      AmendedTransportDocumentStatus expectedAmendmentStatus) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "GET TD (amended content)",
        200,
        true);
    this.responseSchemaValidator = responseSchemaValidator;
    this.expectedAmendmentStatus = expectedAmendmentStatus;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", getDspSupplier().get().transportDocumentReference());
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()),
        "prompt-shipper-get-td-amendment.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        String tdr = getDspSupplier().get().transportDocumentReference();
        List<JsonRebasableContentCheck> nestedChecks =
            new ArrayList<>(EblChecks.amendedTransportDocumentCarrierContentChecks(getDspSupplier()));
        nestedChecks.addAll(
            TransportDocumentStatusScenario.primaryStatusesOnly(PRIMARY_STATUSES).checks(false));
        return Stream.of(
            new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "GET"),
            new UrlPathCheck(
                EblRole::isShipper,
                getMatchedExchangeUuid(),
                "/v3/transport-documents/%s/amendment".formatted(tdr)),
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
            EblChecks.amendedTransportDocumentStatusChecks(
                getMatchedExchangeUuid(), expectedApiVersion, expectedAmendmentStatus),
            JsonAttribute.contentChecks(
                "[Amended Transport Document]",
                "The amended Transport Document has valid content",
                EblRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion,
                JsonContentCheckRebaser.of("amendedTransportDocument"),
                nestedChecks));
      }
    };
  }
}

