package org.dcsa.conformance.gateway.scenarios;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public abstract class ScenarioListBuilder {
  private final ScenarioListBuilder parent;
  private final LinkedList<ScenarioListBuilder> children = new LinkedList<>();
  protected final Function<LinkedList<ConformanceAction>, ConformanceAction> actionBuilder;

  protected ScenarioListBuilder(
      ScenarioListBuilder parent,
      Function<LinkedList<ConformanceAction>, ConformanceAction> actionBuilder) {
    this.parent = parent;
    this.actionBuilder = actionBuilder;
  }

  public List<ConformanceScenario> buildList() {
    return buildTree().buildScenarioList();
  }

  abstract protected ScenarioListBuilder buildTree();

  protected List<ConformanceScenario> buildScenarioList() {
    return parent != null
        ? parent.buildScenarioList()
        : asBuilderListList().stream()
            .map(
                builderList -> {
                  LinkedList<ConformanceAction> actionList = new LinkedList<>();
                  builderList.forEach(
                      builder -> actionList.addLast(builder.actionBuilder.apply(actionList)));
                  return new ConformanceScenario(actionList);
                })
            .toList();
  }

  private LinkedList<LinkedList<ScenarioListBuilder>> asBuilderListList() {
    return new LinkedList<>(
        children.isEmpty()
            ? List.of(new LinkedList<>(List.of(this)))
            : children.stream()
                .flatMap(scenarioListBuilder -> scenarioListBuilder.asBuilderListList().stream())
                .peek(scenarioBuilderList -> scenarioBuilderList.addFirst(this))
                .toList());
  }

  protected ScenarioListBuilder thenEither(
      ScenarioListBuilder... scenarioListBuilders) {
    if (!this.children.isEmpty()) throw new IllegalStateException();
    this.children.addAll(Arrays.asList(scenarioListBuilders));
    return this;
  }

  protected <T extends ScenarioListBuilder> T addChildIfFirst(T child) {
    if (!this.children.isEmpty()) throw new IllegalStateException();
    this.children.add(child);
    return child;
  }
}
