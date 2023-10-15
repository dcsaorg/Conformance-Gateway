package org.dcsa.conformance.standards.eblsurrender.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class SurrenderRequestAction extends TdrAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean forAmendment;

  private final AtomicReference<String> surrenderRequestReference = new AtomicReference<>();

  private final Supplier<String> srrSupplier = surrenderRequestReference::get;

  public SurrenderRequestAction(
      boolean forAmendment,
      String platformPartyName,
      String carrierPartyName,
      int expectedStatus,
      ConformanceAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        platformPartyName,
        carrierPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(forAmendment ? "AREQ" : "SREQ", expectedStatus));
    this.forAmendment = forAmendment;
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
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
    return ("Send a surrender request for %s "
            + "for the eBL with the transport document reference '%s'")
        .formatted(forAmendment ? "amendment" : "delivery", tdrSupplier.get());
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    return srrSupplier;
  }

  @Override
  public ObjectNode asJsonNode() {
    // don't include srr because it's not known when this is sent out
    return super.asJsonNode().put("forAmendment", forAmendment);
  }

  @Override
  public void doHandleExchange(ConformanceExchange exchange) {
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
        return Stream.of(
            new UrlPathCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                "/v1/surrender-requests"),
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
            new JsonSchemaCheck(
                EblSurrenderRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new JsonAttributeCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                "surrenderRequestCode",
                forAmendment ? "AREQ" : "SREQ"),
            new JsonAttributeCheck(
                EblSurrenderRole::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                "transportDocumentReference",
                tdrSupplier.get()));
      }
    };
  }
}
