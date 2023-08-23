package org.dcsa.conformance.gateway.check;

import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

import java.util.LinkedList;
import java.util.stream.Stream;

public class ActionCheck extends ConformanceCheck {
  protected final ActionCheck parent;
  private final LinkedList<ActionCheck> children = new LinkedList<>();

  private final LinkedList<LinkedList<ConformanceExchange>> relevantExchangeLists =
      new LinkedList<>();

  public ActionCheck(String title, ActionCheck parent) {
    super(title);
    this.parent = parent;
    if (parent != null) {
      this.parent.children.add(this);
    }
  }

  protected void addRelevantExchangeList(LinkedList<ConformanceExchange> exchangeList) {
    relevantExchangeLists.add(exchangeList);
  }

  public Stream<LinkedList<ConformanceExchange>> relevantExchangeListsStream() {
    return relevantExchangeLists.stream();
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return children.stream();
  }

  protected boolean hasChildren() {
    return !children.isEmpty();
  }
}
