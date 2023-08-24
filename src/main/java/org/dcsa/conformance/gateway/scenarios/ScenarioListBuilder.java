package org.dcsa.conformance.gateway.scenarios;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.dcsa.conformance.gateway.check.ActionCheck;

public abstract class ScenarioListBuilder {
  protected ScenarioListBuilder parent;
  private final LinkedList<ScenarioListBuilder> children = new LinkedList<>();
  protected final Function<ConformanceAction, ConformanceAction> actionBuilder;
  protected final Function<ActionCheck, ActionCheck> checkBuilder;

  protected ScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder,
      Function<ActionCheck, ActionCheck> checkBuilder) {
    this.actionBuilder = actionBuilder;
    this.checkBuilder = checkBuilder;
  }

  public List<ConformanceScenario> buildList() {
    return buildScenarioList();
  }

  public ActionCheck buildCheckTree() {
    return parent != null ? parent.buildCheckTree() : _buildCheckTree(null);
  }

  private ActionCheck _buildCheckTree(ActionCheck parentActionCheck) {
    ActionCheck actionCheck = checkBuilder.apply(parentActionCheck);
    children.forEach(child -> child._buildCheckTree(actionCheck));
    return actionCheck;
  }

  protected List<ConformanceScenario> buildScenarioList() {
    return parent != null
        ? parent.buildScenarioList()
        : asBuilderListList().stream()
            .map(
                builderList -> {
                  LinkedList<ConformanceAction> actionList = new LinkedList<>();
                  builderList.forEach(
                      builder ->
                          actionList.addLast(builder.actionBuilder.apply(actionList.peekLast())));
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

  protected ScenarioListBuilder then(ScenarioListBuilder child) {
    return thenEither(child);
  }

  protected ScenarioListBuilder thenEither(ScenarioListBuilder... children) {
    if (!this.children.isEmpty()) throw new IllegalStateException();
    Stream.of(children).forEach(
        child -> {
          if (child.parent != null) throw new IllegalStateException();
        });
    this.children.addAll(Arrays.asList(children));
    this.children.forEach(child -> child.parent = this);
    return this;
  }
}
