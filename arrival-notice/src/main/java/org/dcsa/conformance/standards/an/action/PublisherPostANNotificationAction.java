package org.dcsa.conformance.standards.an.action;


public class PublisherPostANNotificationAction extends AnAction {
  protected PublisherPostANNotificationAction(
      String sourcePartyName,
      String targetPartyName,
      AnAction previousAction,
      String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
  }
}
