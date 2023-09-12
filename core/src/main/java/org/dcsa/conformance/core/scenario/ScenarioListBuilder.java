package org.dcsa.conformance.core.scenario;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;

@Slf4j
public abstract class ScenarioListBuilder<T extends ScenarioListBuilder<T>> {
  protected T parent;
  private final LinkedList<T> children = new LinkedList<>();
  protected final Function<ConformanceAction, ConformanceAction> actionBuilder;
  protected final Function<ActionCheck, ActionCheck> checkBuilder;

  protected boolean checkExclusively = false;
  protected boolean runExclusively = false;

  protected ScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder,
      Function<ActionCheck, ActionCheck> checkBuilder) {
    this.actionBuilder = actionBuilder;
    this.checkBuilder = checkBuilder;
  }

  @SuppressWarnings("unchecked")
  protected T thisAsT() {
    return (T)this;
  }

  public ActionCheck buildRootCheckTree() {
    if (parent != null) return parent.buildRootCheckTree();
    _updateExclusiveCheckFlag();
    return _buildCheckTree(null, !checkExclusively);
  }

  protected void _updateExclusiveCheckFlag() {
    children.forEach(child -> {
      child._updateExclusiveCheckFlag();
      if (child.checkExclusively) this.checkExclusively = true;
    });
  }

  protected ActionCheck _buildCheckTree(ActionCheck parentActionCheck, boolean includeNonExclusive) {
    ActionCheck actionCheck = checkBuilder.apply(parentActionCheck);
    children.stream()
        .filter(child -> includeNonExclusive || child.checkExclusively)
        .forEach(child -> child._buildCheckTree(actionCheck, includeNonExclusive));
    return actionCheck;
  }

  public List<ConformanceScenario> buildScenarioList() {
    return _buildScenarioList();
  }

  protected List<ConformanceScenario> _buildScenarioList() {
    return parent != null
        ? parent._buildScenarioList()
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

  protected LinkedList<LinkedList<T>> asBuilderListList() {
    List<LinkedList<T>> builderListList =
        children.isEmpty()
            ? List.of(new LinkedList<>(List.of(thisAsT())))
            : children.stream()
                .flatMap(scenarioListBuilder -> scenarioListBuilder.asBuilderListList().stream())
                .peek(scenarioBuilderList -> scenarioBuilderList.addFirst(thisAsT()))
                .toList();
    List<LinkedList<T>> exclusiveListList =
        builderListList.stream()
            .filter(builderList -> Objects.requireNonNull(builderList.peekLast()).runExclusively)
            .toList();
    return new LinkedList<>(exclusiveListList.isEmpty() ? builderListList : exclusiveListList);
  }

  protected T then(T child) {
    log.info("ScenarioListBuilder.then()");
    return thenEither(child);
  }

  @SafeVarargs
  protected final T thenEither(T... children) {
    log.info("ScenarioListBuilder.thenEither(%d)".formatted(children.length));
    if (!this.children.isEmpty()) throw new IllegalStateException();
    Stream.of(children)
        .forEach(
            child -> {
              if (child.parent != null) {
                throw new IllegalStateException();
              }
            });
    this.children.addAll(Arrays.asList(children));
    this.children.forEach(child -> child.parent = thisAsT());
    return thisAsT();
  }

  protected T runAndCheckExclusively() {
    if (!children.isEmpty()) throw new IllegalStateException();
    this.runExclusively = true;
    this.checkExclusively = true;
    return thisAsT();
  }
}
