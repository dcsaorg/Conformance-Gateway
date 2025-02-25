package org.dcsa.conformance.standards.jit.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
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
  public static final Set<String> MULTIPLE_RESULTS_URL_PARAMS =
      Set.of(
          "UNLocationCode",
          "carrierServiceName",
          "carrierServiceCode",
          "vesselIMONumber",
          "vesselName",
          "MMSINumber",
          "universalServiceReference",
          "carrierImportVoyageNumber",
          "carrierExportVoyageNumber",
          "universalImportVoyageReference",
          "universalExportVoyageReference");

  private final JitGetType getType;
  private final JsonSchemaValidator validator;
  private final boolean requestedByProvider;
  private final List<String> urlFilters;
  private final int expectedResults;
  private final boolean moreResultsAllowed;

  public JitGetAction(
      JitScenarioContext context,
      ConformanceAction previousAction,
      JitGetType getType,
      List<String> urlFilters,
      boolean requestedByProvider,
      int expectedResults) {
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
    this.expectedResults = expectedResults;
    this.moreResultsAllowed = determineMoreResultsAllowed(urlFilters);
    validator = context.componentFactory().getMessageSchemaValidator(getType.getJitSchema());
  }

  // Some GET requests might return multiple results. This method determines if that is allowed,
  // depending on the applied URL filter(s). Likely, implementers have example data in their
  // systems, so it is possible that other results are returned. This method is a (good enough)
  // effort to determine if multiple results are allowed.
  private boolean determineMoreResultsAllowed(List<String> urlFilters) {
    if (urlFilters == null || urlFilters.isEmpty()) return true; // No filters applied
    if (urlFilters.size() == 1) return MULTIPLE_RESULTS_URL_PARAMS.contains(urlFilters.getFirst());

    return false; // Specifying multiple filters result in a strict match
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
    return getMarkdownFile(
        "prompt-get-request.md", Map.of("GET_TYPE_PLACEHOLDER", getType.getName()));
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        if (dsp == null) return Stream.of();
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
              JsonAttribute.contentChecks(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  expectedApiVersion,
                  JitChecks.checkExpectedResultCount(expectedResults, moreResultsAllowed)),
              new JsonSchemaCheck(
                  JitRole::isConsumer,
                  getMatchedExchangeUuid(),
                  HttpMessageType.RESPONSE,
                  validator));
        }
        // Service Consumer sends request
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
            JsonAttribute.contentChecks(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion,
                JitChecks.checkExpectedResultCount(expectedResults, moreResultsAllowed)),
            new JsonSchemaCheck(
                JitRole::isProvider,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                validator));
      }
    };
  }
}
