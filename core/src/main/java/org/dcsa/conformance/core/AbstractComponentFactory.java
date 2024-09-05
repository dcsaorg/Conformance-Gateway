package org.dcsa.conformance.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

@Getter
public abstract class AbstractComponentFactory {

  protected static final String AUTH_HEADER_VALUE_ROLE_ONE = UUID.randomUUID().toString();
  protected static final String AUTH_HEADER_VALUE_ROLE_TWO = UUID.randomUUID().toString();

  protected final String standardName;
  protected final String standardVersion;
  protected final String scenarioSuite;
  protected final String roleOne;
  protected final String roleTwo;

  protected AbstractComponentFactory(
      String standardName,
      String standardVersion,
      String scenarioSuite,
      String roleOne,
      String roleTwo) {
    this.standardName = standardName;
    this.standardVersion = standardVersion;
    this.scenarioSuite = scenarioSuite;
    this.roleOne = roleOne;
    this.roleTwo = roleTwo;
  }

  public abstract List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader);

  /**
   * Creates the ScenarioListBuilders of each standard module.
   *
   * <p>For a standard without modules, return a single-entry map with "" as key:
   *
   * <p><code>
   * return Stream.of(Map.entry("", yourRootScenarioListBuilder)).collect(Collectors.toMap(
   *    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
   * </code>
   */
  protected abstract LinkedHashMap<String, ? extends ScenarioListBuilder<?>>
      createModuleScenarioListBuilders(
          PartyConfiguration[] partyConfigurations,
          CounterpartConfiguration[] counterpartConfigurations);

  public void generateConformanceScenarios(
    Map<String, List<ConformanceScenario>> scenariosByModuleName,
    PartyConfiguration[] partyConfigurations,
    CounterpartConfiguration[] counterpartConfigurations
  ) {
    LinkedHashMap<String, ? extends ScenarioListBuilder<?>> moduleScenarioListBuilders =
      this.createModuleScenarioListBuilders(partyConfigurations, counterpartConfigurations);
    AtomicInteger nextModuleIndex = new AtomicInteger();
    moduleScenarioListBuilders.forEach(
      (moduleName, scenarioListBuilder) -> {
        var moduleScenarios = new ArrayList<ConformanceScenario>(scenarioListBuilder.buildScenarioList(nextModuleIndex.getAndIncrement()));
        scenariosByModuleName.put(moduleName, moduleScenarios);
      });
  }

  public abstract SortedSet<String> getRoleNames();

  public abstract Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations);

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {

    String sandboxIdPrefix =
        AbstractComponentFactory._sandboxIdPrefix(standardName, standardVersion, scenarioSuite);

    boolean isAllInOneSandbox = testedPartyRole == null;
    boolean isRoleOneInSandbox =
        (roleTwo.equals(testedPartyRole) && isTestingCounterpartsConfig)
            || (roleOne.equals(testedPartyRole) && !isTestingCounterpartsConfig);
    boolean isRoleTwoInSandbox =
        (roleOne.equals(testedPartyRole) && isTestingCounterpartsConfig)
            || (roleTwo.equals(testedPartyRole) && !isTestingCounterpartsConfig);

    String autoOrManualInfix = isManual ? "manual" : "auto";

    String sandboxIdSuffix =
        isAllInOneSandbox
            ? "auto-all-in-one"
            : "%s-%s-%s"
                .formatted(
                    autoOrManualInfix,
                    testedPartyRole.toLowerCase(),
                    isTestingCounterpartsConfig ? "testing-counterparts" : "tested-party");

    ObjectNode sandboxNode = JsonToolkit.OBJECT_MAPPER.createObjectNode();

    String sandboxIdAndName = "%s-%s".formatted(sandboxIdPrefix, sandboxIdSuffix);
    sandboxNode.put("id", sandboxIdAndName);
    sandboxNode.put("name", sandboxIdAndName);

    if (!isManual) {
      sandboxNode.put("authHeaderName", "dcsa-conformance-api-key");
      boolean useRoleTwoAuth = !isAllInOneSandbox && isRoleTwoInSandbox;
      sandboxNode.put(
          "authHeaderValue",
          useRoleTwoAuth ? AUTH_HEADER_VALUE_ROLE_TWO : AUTH_HEADER_VALUE_ROLE_ONE);
    }

    sandboxNode.set(
        "standard",
        JsonToolkit.OBJECT_MAPPER
            .createObjectNode()
            .put("name", standardName)
            .put("version", standardVersion));

    sandboxNode.put("scenarioSuite", scenarioSuite);

    sandboxNode.set(
        "orchestrator",
        JsonToolkit.OBJECT_MAPPER
            .createObjectNode()
            .put("active", !isManual || isTestingCounterpartsConfig));

    Map<String, Boolean> isRoleInSandbox =
        Map.ofEntries(
            Map.entry(roleOne, isRoleOneInSandbox), Map.entry(roleTwo, isRoleTwoInSandbox));

    ArrayNode partiesNode = JsonToolkit.OBJECT_MAPPER.createArrayNode();
    sandboxNode.set("parties", partiesNode);
    ArrayNode counterpartsNode = JsonToolkit.OBJECT_MAPPER.createArrayNode();
    sandboxNode.set("counterparts", counterpartsNode);
    Stream.of(roleOne, roleTwo)
        .forEach(
            roleName -> {
              if (isAllInOneSandbox || isRoleInSandbox.get(roleName)) { // party node
                ObjectNode rolePartyNode =
                    JsonToolkit.OBJECT_MAPPER
                        .createObjectNode()
                        .put("name", roleName + "1")
                        .put("role", roleName)
                        .put(
                            "orchestratorUrl",
                            "http://localhost:8080/conformance/sandbox/%s-%s-%s"
                                .formatted(
                                    sandboxIdPrefix,
                                    autoOrManualInfix,
                                    isAllInOneSandbox
                                        ? "all-in-one"
                                        : "%s-testing-counterparts"
                                            .formatted(
                                                (Objects.equals(testedPartyRole, roleOne)
                                                        ? roleOne
                                                        : roleTwo)
                                                    .toLowerCase())));
                if (isManual && roleName.equals(testedPartyRole))
                  rolePartyNode.put("inManualMode", true);
                partiesNode.add(rolePartyNode);
              }

              { // counterpart node
                ObjectNode roleCounterpartNode =
                    JsonToolkit.OBJECT_MAPPER
                        .createObjectNode()
                        .put("name", roleName + "1")
                        .put("role", roleName)
                        .put(
                            "url",
                            "http://localhost:8080/conformance/sandbox/%s-%s-%s/party/%s1/api"
                                .formatted(
                                    sandboxIdPrefix,
                                    autoOrManualInfix,
                                    isAllInOneSandbox
                                        ? "all-in-one"
                                        : "%s-%s"
                                            .formatted(
                                                testedPartyRole.toLowerCase(),
                                                roleName.equals(testedPartyRole)
                                                    ? "tested-party"
                                                    : "testing-counterparts"),
                                    roleName));
                if (isManual && roleName.equals(testedPartyRole))
                  roleCounterpartNode.put("inManualMode", true);
                if (!isManual) {
                  roleCounterpartNode.put("authHeaderName", "dcsa-conformance-api-key");
                  roleCounterpartNode.put(
                      "authHeaderValue",
                      isAllInOneSandbox || roleName.equals(roleOne)
                          ? AUTH_HEADER_VALUE_ROLE_ONE
                          : AUTH_HEADER_VALUE_ROLE_TWO);
                }
                counterpartsNode.add(roleCounterpartNode);
              }
            });
    return sandboxNode;
  }

  protected static String _findPartyOrCounterpartName(
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

  protected static String _sandboxIdPrefix(
      String standardName, String standardVersion, String scenarioSuite) {
    return "%s-%s-%s"
        .formatted(
            standardName.replace(" ", ""),
            standardVersion.replace(".", "").replace("-", ""),
            scenarioSuite.replace(" ", "-"))
        .toLowerCase();
  }
}
