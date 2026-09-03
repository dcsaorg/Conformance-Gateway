package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;
import org.dcsa.conformance.standards.ebl.party.EblRole;

public class UC18_Shipper_CancelTransportDocumentAmendmentAction extends ShipperNotificationEblAction {
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC18_Shipper_CancelTransportDocumentAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "UC18",
        202,
        isWithNotifications);
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()),
        "prompt-shipper-uc18.md",
        "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", getDspSupplier().get().transportDocumentReference());
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        String tdr = getDspSupplier().get().transportDocumentReference();
        Stream<ConformanceCheck> requestChecks =
            Stream.of(
                new HttpMethodCheck(EblRole::isShipper, getMatchedExchangeUuid(), "DELETE"),
                new UrlPathCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    "/v3/transport-documents/%s/amendment".formatted(tdr)),
                ResponseStatusCheck.forSuccessfulResponse(
                    EblRole::isCarrier, getMatchedExchangeUuid()),
                new ApiHeaderCheck(
                    EblRole::isShipper,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    expectedApiVersion),
                new ApiHeaderCheck(
                    EblRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    expectedApiVersion));
        return Stream.concat(
            requestChecks,
            getTDNotificationChecks(
                getMatchedNotificationExchangeUuid(),
                expectedApiVersion,
                notificationSchemaValidator,
                TransportDocumentStatusScenario.uc18()));
      }
    };
  }
}

