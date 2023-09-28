package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;

@Slf4j
public abstract class ConformanceOrchestrator implements StatefulEntity {
  public static ConformanceOrchestrator createOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    return sandboxConfiguration.getOrchestrator().getMaxParallelScenarios() > 0
        ? new AutomaticOrchestrator(
            sandboxConfiguration, componentFactory, trafficRecorder, asyncWebClient)
        : new ManualOrchestrator(
            sandboxConfiguration, componentFactory, trafficRecorder, asyncWebClient);
  }

  protected final SandboxConfiguration sandboxConfiguration;
  protected final ComponentFactory componentFactory;
  protected final TrafficRecorder trafficRecorder;
  private final Consumer<ConformanceWebRequest> asyncWebClient;

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.componentFactory = componentFactory;
    this.trafficRecorder = trafficRecorder;
    this.asyncWebClient = asyncWebClient;
  }

  public abstract JsonNode getStatus();

  public abstract void notifyRelevantParties();

  protected void _asyncNotifyParty(String partyName) {
    log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
    CounterpartConfiguration counterpartConfiguration =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()))
            .get(partyName);
    String uri =
        counterpartConfiguration.getRootPath() + "/party/%s/notification".formatted(partyName);
    String url = counterpartConfiguration.getBaseUrl() + uri;
    asyncWebClient.accept(
        new ConformanceWebRequest(
            "GET", url, uri, Collections.emptyMap(), Collections.emptyMap(), ""));
  }

  public abstract JsonNode handleGetPartyPrompt(String partyName);

  public abstract void handlePartyInput(JsonNode partyInput);

  public abstract void handlePartyTrafficExchange(ConformanceExchange exchange);

  public String generateReport(Set<String> roleNames) {
    ConformanceCheck conformanceCheck =
        componentFactory.createConformanceCheck(
            componentFactory.createScenarioListBuilder(
                sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts()));
    trafficRecorder.getTrafficStream().forEach(conformanceCheck::check);
    Map<String, ConformanceReport> reportsByRoleName =
        ConformanceReport.createForRoles(conformanceCheck, roleNames);

    return ConformanceReport.toHtmlReport(reportsByRoleName);
  }
}
