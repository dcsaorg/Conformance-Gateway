package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.jit.JitScenarioContext;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitGetType;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
public class JitGetAction extends JitAction {
  public static final String GET_TYPE = "getType";
  public static final String FILTERS = "filters";

  private final JitGetType getType;
  private final JsonSchemaValidator validator;
  private final boolean requestedByProvider;
  private final List<String> urlFilters;

  public JitGetAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      JitGetType getType,
      List<String> urlFilters,
      boolean requestedByProvider) {
    super(
        requestedByProvider ? context.providerPartyName() : context.consumerPartyName(),
        requestedByProvider ? context.consumerPartyName() : context.providerPartyName(),
        previousAction,
        requestedByProvider
            ? "Send GET %s by %s".formatted(getType.getName(), urlFilters)
            : "Receive GET %s by %s".formatted(getType.getName(), urlFilters));
    this.getType = getType;
    this.urlFilters = urlFilters;
    this.requestedByProvider = requestedByProvider;
    validator = context.componentFactory().getMessageSchemaValidator(getType.getJitSchema());
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put(GET_TYPE, getType.name());
    jsonNode.putPOJO(FILTERS, urlFilters);
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Get %s (GET) request".formatted(getType);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (dsp == null) return Stream.of();
        ActionCheck checksForTimestamp =
            JitChecks.createChecksForTimestamp(
                JitRole::isProvider, getMatchedExchangeUuid(), expectedApiVersion, dsp);
        if (requestedByProvider) {
          return Stream.of(
              new UrlPathCheck(JitRole::isProvider, getMatchedExchangeUuid(), getType.getUrlPath()),
              new HttpMethodCheck(JitRole::isProvider, getMatchedExchangeUuid(), JitStandard.GET),
              new ResponseStatusCheck(JitRole::isConsumer, getMatchedExchangeUuid(), 200),
              new ApiHeaderCheck(
                  JitRole::isProvider,
                  getMatchedExchangeUuid(),
                  HttpMessageType.REQUEST,
                  expectedApiVersion),
              new ApiHeaderCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  expectedApiVersion),
              new JsonSchemaCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  validator));
        }
        // Consumer sends request
        return Stream.of(
            new UrlPathCheck(JitRole::isConsumer, getMatchedExchangeUuid(), getType.getUrlPath()),
            new HttpMethodCheck(JitRole::isConsumer, getMatchedExchangeUuid(), JitStandard.GET),
            new ResponseStatusCheck(JitRole::isProvider, getMatchedExchangeUuid(), 200),
            new ApiHeaderCheck(
                JitRole::isConsumer,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                validator));
      }
    };
  }
}
