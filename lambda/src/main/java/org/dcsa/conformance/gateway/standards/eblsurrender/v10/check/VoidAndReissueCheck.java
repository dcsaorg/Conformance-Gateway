package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

import java.util.LinkedList;
import java.util.stream.Stream;

public class VoidAndReissueCheck extends ActionCheck {
  public VoidAndReissueCheck(String title, ActionCheck parent) {
    super(title, parent);
  }

  @Override
  public Stream<LinkedList<ConformanceExchange>> relevantExchangeListsStream() {
    return parent.relevantExchangeListsStream();
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return childrenStream().anyMatch(child -> child.isRelevantForRole(roleName));
  }
}
