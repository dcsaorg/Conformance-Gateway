package org.dcsa.conformance.standards.tnt.v300.action;

public class ConsumerGetTntAction extends TntAction {

  protected ConsumerGetTntAction(
      String sourcePartyName,
      String targetPartyName,
      TntAction previousAction,
      TntEventType eventType) {
    super(sourcePartyName, targetPartyName, previousAction, "Consumer GET Action for " + eventType);
  }
}
