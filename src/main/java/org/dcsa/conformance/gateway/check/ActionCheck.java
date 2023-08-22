package org.dcsa.conformance.gateway.check;

import java.util.LinkedList;
import java.util.stream.Stream;

public class ActionCheck extends ConformanceCheck {
  private final ActionCheck parent;
  private final LinkedList<ActionCheck> children = new LinkedList<>();

  public ActionCheck(String title, ActionCheck parent) {
    super(title);
    this.parent = parent;
    if (parent != null) {
      this.parent.children.add(this);
    }
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return children.stream();
  }
}
