package org.dcsa.conformance.core.scenario;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ScenarioListBuilder<T extends ScenarioListBuilder<T>> {
  protected T parent;
  private final LinkedList<T> children = new LinkedList<>();
  protected final Function<ConformanceAction, ConformanceAction> actionBuilder;

  protected ScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    this.actionBuilder = actionBuilder;
  }

  @SuppressWarnings("unchecked")
  protected T thisAsT() {
    return (T) this;
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
                  builderList.stream()
                      .filter(builder -> builder.actionBuilder != null)
                      .forEach(
                          builder ->
                              actionList.addLast(
                                  builder.actionBuilder.apply(actionList.peekLast())));
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
    return new LinkedList<>(builderListList);
  }

  protected T then(T child) {
    log.debug("ScenarioListBuilder.then()");
    return thenEither(child);
  }

  @SafeVarargs
  protected final T thenEither(T... children) {
    log.debug("ScenarioListBuilder.thenEither(%d)".formatted(children.length));
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
}
