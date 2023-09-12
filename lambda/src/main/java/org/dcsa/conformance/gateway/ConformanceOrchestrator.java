package org.dcsa.conformance.gateway;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.gateway.configuration.ConformanceConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10ConformanceCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.VoidAndReissueAction;

@Slf4j
public class ConformanceOrchestrator {
  private final boolean inactive;
  private final ConformanceConfiguration conformanceConfiguration;
  protected final ScenarioListBuilder<?> scenarioListBuilder;
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();
  private final ConformanceTrafficRecorder trafficRecorder;
  private final Map<String, CounterpartConfiguration> counterpartConfigurationsByPartyName;

  public ConformanceOrchestrator(ConformanceConfiguration conformanceConfiguration) {
    this.inactive = conformanceConfiguration.getOrchestrator() == null;
    this.conformanceConfiguration = conformanceConfiguration;

    this.scenarioListBuilder =
        inactive
            ? null
            : createScenarioListBuilder(
                conformanceConfiguration.getStandard(),
                conformanceConfiguration.getParties(),
                conformanceConfiguration.getCounterparts());

    trafficRecorder = inactive ? null : new ConformanceTrafficRecorder();

    counterpartConfigurationsByPartyName =
        Arrays.stream(conformanceConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()));
  }

  public JsonNode reset() {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    trafficRecorder.reset();

    scenarios.clear();
    scenarios.addAll(scenarioListBuilder.buildScenarioList());

    notifyAllPartiesOfNextActions();

    return new ObjectMapper().createObjectNode();
  }

  private synchronized void notifyAllPartiesOfNextActions() {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    scenarios.stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::asyncNotifyParty);
  }

  private void asyncNotifyParty(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
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
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
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
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
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

  public synchronized JsonNode handlePartyInput(JsonNode partyInput) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
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
    return new ObjectMapper().createObjectNode();
  }

  public synchronized void handlePartyTrafficExchange(ConformanceExchange exchange) {
    if (inactive) return;
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
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");

    ConformanceCheck conformanceCheck =
        _createConformanceCheck(conformanceConfiguration.getStandard(), scenarioListBuilder);
    trafficRecorder.getTrafficStream().forEach(conformanceCheck::check);
    Map<String, ConformanceReport> reportsByRoleName =
        ConformanceReport.createForRoles(conformanceCheck, roleNames);

    return ConformanceReport.toHtmlReport(reportsByRoleName);
  }

  private static ConformanceCheck _createConformanceCheck(
      StandardConfiguration standardConfiguration, ScenarioListBuilder<?> scenarioListBuilder) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return new EblSurrenderV10ConformanceCheck(scenarioListBuilder);
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }

  public static ScenarioListBuilder<?> createScenarioListBuilder(
      StandardConfiguration standardConfiguration,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return EblSurrenderV10ScenarioListBuilder.buildTree(
          _findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isCarrier),
          _findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isPlatform));
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }

  private static String _findPartyOrCounterpartName(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      Predicate<String> rolePredicate) {
    return Stream.concat(
            Arrays.stream(partyConfigurations)
                .filter(partyConfiguration -> rolePredicate.test(partyConfiguration.getRole()))
                .map(PartyConfiguration::getName),
            Arrays.stream(counterpartConfigurations)
                .filter(
                    counterpartConfigurationConfiguration ->
                        rolePredicate.test(counterpartConfigurationConfiguration.getRole()))
                .map(CounterpartConfiguration::getName))
        .findFirst()
        .orElseThrow();
  }
}
