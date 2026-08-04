package org.dcsa.conformance.standards.ovs.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ovs.checks.OvsChecks;
import org.dcsa.conformance.standards.ovs.checks.QueryParameterSchemaCheck;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

import java.util.stream.Stream;

@Getter
@Slf4j
public class OvsGetSchedulesAction extends OvsAction {
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean includeResponseContentChecks;

  public OvsGetSchedulesAction(
    String subscriberPartyName,
    String publisherPartyName,
    ConformanceAction previousAction,
    JsonSchemaValidator responseSchemaValidator,
    boolean includeResponseContentChecks) {
    super(
      subscriberPartyName,
      publisherPartyName,
      previousAction,
      previousAction instanceof OvsGetSchedulesAction
        ? "GET service schedules (cursor)"
        : "GET service schedules",
      200);
    this.responseSchemaValidator = responseSchemaValidator;
    this.includeResponseContentChecks = includeResponseContentChecks;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (previousAction instanceof OvsGetSchedulesAction) {
      return "Send a GET service schedules request using only the cursor query parameter set to "
        + "the value returned in the previous Next-Page-Cursor response header.";
    }
    if (sspSupplier.get().getMap().isEmpty()) {
      return "Send a GET service schedules request.";
    }
    return "Send a GET service schedules request with the following parameters: "
      + sspSupplier.get().toJson().toPrettyString();
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var defaultChecks =
          Stream.of(
            new UrlPathCheck(OvsRole::isSubscriber, getMatchedExchangeUuid(), "/service-schedules"),
            new ApiHeaderCheck(
              OvsRole::isSubscriber,
              getMatchedExchangeUuid(),
              HttpMessageType.REQUEST,
              expectedApiVersion),
            new ApiHeaderCheck(
              OvsRole::isPublisher,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              expectedApiVersion),
            new ResponseStatusCheck(OvsRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new QueryParameterSchemaCheck(
              "",
              "The query parameters of the HTTP request are correct",
              OvsRole::isSubscriber,
              getMatchedExchangeUuid(),
              "/standards/ovs/schemas/OVS_v3.0.0.yaml"),
            new JsonSchemaCheck(
              OvsRole::isPublisher,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              responseSchemaValidator));
        if (includeResponseContentChecks) {
          return Stream.concat(
            defaultChecks,
            Stream.of(
              OvsChecks.mandatoryResponseContentChecks(getMatchedExchangeUuid(), expectedApiVersion),
              OvsChecks.optionalResponseContentChecks(getMatchedExchangeUuid(), expectedApiVersion)
            )
          );
        }
        return defaultChecks;
      }
    };
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode actionNode = super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
    String cursor = getPaginationCursorSupplier().get();
    if (cursor != null && !cursor.isBlank()) {
      actionNode.put("cursor", cursor);
    }
    return actionNode;
  }
}
