package org.dcsa.conformance.standards.eblsurrender.v10.action;

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
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10Role;

@Getter
@Slf4j
public class SurrenderRequestAction extends TdrAction {
  private final boolean forAmendment;

  private final AtomicReference<String> surrenderRequestReference = new AtomicReference<>();

  private final Supplier<String> srrSupplier = surrenderRequestReference::get;

  public SurrenderRequestAction(
      boolean forAmendment,
      String platformPartyName,
      String carrierPartyName,
      int expectedStatus,
      ConformanceAction previousAction) {
    super(
        platformPartyName,
        carrierPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(forAmendment ? "AREQ" : "SREQ", expectedStatus));
    this.forAmendment = forAmendment;
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

  private static final String SCHEMAS_FOLDER = "/eblsurrender/v10/";
  private static final String SCHEMAS_FILE_ASYNC_REQUEST =
      SCHEMAS_FOLDER + "eblsurrender-v10-async-request.json";
  private static final JsonSchemaValidator requestJsonSchemaValidator =
      new JsonSchemaValidator(SCHEMAS_FILE_ASYNC_REQUEST, "surrenderRequestDetails");
  private final JsonSchemaValidator responseJsonSchemaValidator =
      new JsonSchemaValidator(SCHEMAS_FILE_ASYNC_REQUEST, "surrenderRequestAcknowledgement");

  @Override
  public ConformanceCheck createCheck() {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                EblSurrenderV10Role::isPlatform,
                getMatchedExchangeUuid(),
                "/v1/surrender-requests"),
            new ResponseStatusCheck(
                EblSurrenderV10Role::isCarrier, getMatchedExchangeUuid(), getExpectedStatus()),
            new ApiHeaderCheck(
                EblSurrenderV10Role::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                "1.0.0"),
            new ApiHeaderCheck(
                EblSurrenderV10Role::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                "1.0.0"),
            new JsonSchemaCheck(
                EblSurrenderV10Role::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestJsonSchemaValidator),
            new JsonSchemaCheck(
                EblSurrenderV10Role::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseJsonSchemaValidator),
            new JsonAttributeCheck(
                EblSurrenderV10Role::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                "surrenderRequestCode",
                forAmendment ? "AREQ" : "SREQ"),
            new JsonAttributeCheck(
                EblSurrenderV10Role::isPlatform,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                "transportDocumentReference",
                tdrSupplier.get()));
      }
    };
  }
}
