package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.analysis.ConformanceReport;
import org.dcsa.conformance.gateway.analysis.ConformanceTrafficAnalyzer;
import org.dcsa.conformance.gateway.configuration.CounterpartConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ConformanceScenario;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilderFactory;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.VoidAndReissueAction;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;
import org.dcsa.conformance.gateway.traffic.ConformanceTrafficRecorder;

@Slf4j
public class ConformanceOrchestrator {
  private final StandardConfiguration standardConfiguration;
  protected final ScenarioListBuilder<?> scenarioListBuilder;
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();
  private final ConformanceTrafficRecorder trafficRecorder = new ConformanceTrafficRecorder();
  private final Map<String, CounterpartConfiguration> counterpartConfigurationsByPartyName;

  public ConformanceOrchestrator(
      StandardConfiguration standardConfiguration,
      CounterpartConfiguration[] counterpartConfigurations) {

    this.standardConfiguration = standardConfiguration;
    this.scenarioListBuilder =
        ScenarioListBuilderFactory.create(standardConfiguration, counterpartConfigurations);
    counterpartConfigurationsByPartyName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()));
  }

  public void reset() {
    trafficRecorder.reset();
    initializeScenarios();
    notifyAllPartiesOfNextActions();
  }

  private synchronized void notifyAllPartiesOfNextActions() {
    scenarios.stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::asyncNotifyParty);
  }

  protected synchronized void initializeScenarios() {
    scenarios.clear();
    scenarios.addAll(scenarioListBuilder.buildScenarioList());
  }

  private void asyncNotifyParty(String partyName) {
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
              syncNotifyParty(partyName);
            })
        .exceptionally(
            e -> {
              log.error("Failed to notify party '%s': %s".formatted(partyName, e), e);
              return null;
            });
  }

  @SneakyThrows
  private void syncNotifyParty(String partyName) {
    CounterpartConfiguration counterpartConfiguration =
        counterpartConfigurationsByPartyName.get(partyName);
    HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        counterpartConfiguration.getBaseUrl()
                            + counterpartConfiguration.getRootPath()
                            + "/party/%s/notification".formatted(partyName)))
                .timeout(Duration.ofHours(1))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  public synchronized JsonNode handleGetPartyPrompt(String partyName) {
    log.info("ConformanceOrchestrator.handleGetPartyPrompt(%s)".formatted(partyName));
    return new ObjectMapper()
        .createArrayNode()
        .addAll(
            scenarios.stream()
                .map(ConformanceScenario::peekNextAction)
                .filter(Objects::nonNull)
                .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                .map(ConformanceAction::asJsonNode)
                .collect(Collectors.toList()));
  }

  public synchronized void handlePartyInput(String partyName, JsonNode partyInput) {
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    String actionId = partyInput.get("actionId").asText();
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && Objects.equals(actionId, scenario.peekNextAction().getId().toString()))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Input for already handled(?) actionId %s: %s"
                            .formatted(actionId, partyInput.toPrettyString())));
    if (action instanceof SupplyAvailableTdrAction supplyAvailableTdrAction) {
      supplyAvailableTdrAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else if (action instanceof VoidAndReissueAction voidAndReissueAction) {
      voidAndReissueAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else {
      throw new UnsupportedOperationException(partyInput.toString());
    }
    notifyAllPartiesOfNextActions();
    new ObjectMapper().createObjectNode();
  }

  public synchronized void handlePartyTrafficExchange(ConformanceExchange exchange) {
    trafficRecorder.recordExchange(exchange);
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && scenario.peekNextAction().updateFromExchangeIfItMatches(exchange))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElse(null);
    if (action == null) {
      log.info(
          "Ignoring conformance exchange not matched by any pending actions: %s"
              .formatted(exchange));
      return;
    }
    notifyAllPartiesOfNextActions();
  }

  public String generateReport(Set<String> roleNames) {
    Map<String, ConformanceReport> reportsByRoleName =
        new ConformanceTrafficAnalyzer(standardConfiguration)
            .analyze(scenarioListBuilder, trafficRecorder.getTrafficStream(), roleNames);
    return ConformanceReport.toHtmlReport(reportsByRoleName);
  }
}
