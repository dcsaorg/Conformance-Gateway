package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.dcsa.conformance.standards.an.party.ANRole;

public class SubscriberGetANAction extends ANAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public SubscriberGetANAction(
      String subscriberPartyName,
      String publisherPartyName,
      ANAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GET AN");
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the arrival notices with the given transport document references from the publisher."
        + getDspSupplier().get().transportDocumentReferences().toString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(ANRole::isSubscriber, getMatchedExchangeUuid(), "/arrival-notices"),
            new ResponseStatusCheck(ANRole::isPublisher, getMatchedExchangeUuid(), 200),
            new JsonSchemaCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator),
            new ApiHeaderCheck(
                ANRole::isSubscriber,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                ANRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            ANChecks.getANGetResponseChecks(
                getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()));
      }
    };
  }



  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    List<String> references = getDspSupplier().get().transportDocumentReferences();
    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
    assert references != null;
    for (String ref : references) {
      arrayNode.add(ref);
    }
    jsonNode.set("references", arrayNode);
    return jsonNode;
  }
}
