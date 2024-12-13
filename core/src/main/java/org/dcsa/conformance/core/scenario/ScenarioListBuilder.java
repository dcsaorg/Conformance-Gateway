package org.dcsa.conformance.core.scenario;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.NonNull;
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

  public List<ConformanceScenario> buildScenarioList(long moduleIndex) {
    AtomicInteger nextScenarioIndex = new AtomicInteger();
    return parent != null
        ? parent.buildScenarioList(moduleIndex)
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
                  return new ConformanceScenario(moduleIndex, nextScenarioIndex.getAndIncrement(), actionList);
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
    log.debug("ScenarioListBuilder.thenEither()");
    return thenEither(Collections.singletonList(child));
  }

  @SafeVarargs
  protected final T thenEither(T... newChildren) {
    return thenEither(Arrays.asList(newChildren));
  }

  protected final T thenEither(@NonNull List<T> newChildren) {
    log.debug("ScenarioListBuilder.thenEither({})", newChildren.size());
    if (!children.isEmpty())
      throw new IllegalStateException(
          "Cannot add newChildren to a builder that already has children");
    newChildren.forEach(
        child -> {
          if (child.parent != null) {
            throw new IllegalStateException("Child already has a parent");
          }
        });
    children.addAll(newChildren);
    children.forEach(child -> child.parent = thisAsT());
    return thisAsT();
  }
}
