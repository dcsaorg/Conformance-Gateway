package org.dcsa.conformance.standards.eblsurrender.action;

import static org.dcsa.conformance.standards.eblsurrender.SurrenderChecks.surrenderRequestChecks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

@Getter
@Slf4j
public class SurrenderRequestResponseAction extends EblSurrenderAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean forAmendment;
  private final boolean accept;
  private final boolean isSwitchToPaper;

  private final AtomicReference<String> surrenderRequestReference = new AtomicReference<>();

  private final Supplier<String> srrSupplier = surrenderRequestReference::get;

  public SurrenderRequestResponseAction(
      boolean forAmendment,
      String platformPartyName,
      String carrierPartyName,
      int expectedStatus,
      ConformanceAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      boolean accept,
      String title,
      boolean isSWTP) {
    super(platformPartyName, carrierPartyName, expectedStatus, previousAction, title);
    this.forAmendment = forAmendment;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.accept = accept;
    this.isSwitchToPaper = isSWTP;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    String srr = surrenderRequestReference.get();
    if (srr != null) {
      jsonState.put("surrenderRequestReference", srr);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode srrNode = jsonState.get("surrenderRequestReference");
    if (srrNode != null) {
      surrenderRequestReference.set(srrNode.asText());
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "SURRENDER_TYPE",
            forAmendment ? "amendment" : "delivery",
            "REFERENCE",
            sspSupplier.get().transportDocumentReference()),
        "prompt-surrender-reqres.md");
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    return srrSupplier;
  }

  @Override
  public ObjectNode asJsonNode() {
    // don't include srr because it's not known when this is sent out
    return super.asJsonNode()
        .put("forAmendment", forAmendment)
        .put("isSwitchToPaper", isSwitchToPaper);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String srr = requestJsonNode.get("surrenderRequestReference").asText();
    log.info("Updating SurrenderRequestAction '%s' with SRR '%s'".formatted(getActionTitle(), srr));
    this.surrenderRequestReference.set(srr);
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
                new JsonAttributeCheck(
                    EblSurrenderRole::isPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/surrenderRequestCode"),
                    forAmendment ? "AREQ" : "SREQ"),
                new JsonAttributeCheck(
                    EblSurrenderRole::isPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/reasonCode"),
                    forAmendment && isSwitchToPaper ? "SWTP" : ""),
                surrenderRequestChecks(getMatchedExchangeUuid(), expectedApiVersion),
                new JsonAttributeCheck(
                    EblSurrenderRole::isPlatform,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/transportDocumentReference"),
                    sspSupplier.get() == null
                        ? null
                        : sspSupplier.get().transportDocumentReference())),
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
                new JsonAttributeCheck(
                    "[Response]",
                    EblSurrenderRole::isCarrier,
                    getMatchedNotificationExchangeUuid(),
                    HttpMessageType.REQUEST,
                    JsonPointer.compile("/org/dcsa/conformance/standards/standardscommons"),
                    accept ? "SURR" : "SREJ")));
      }
    };
  }
}
