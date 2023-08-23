package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.gateway.check.ActionCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.toolkit.JsonToolkit;

public class SurrenderResponseCheck extends TdrActionCheck {
  private final boolean accept;

  public SurrenderResponseCheck(
      String title, ActionCheck parent, boolean accept, int expectedStatus) {
    super(title, parent, expectedStatus, EblSurrenderV10Role::isPlatform);
    this.accept = accept;
  }

  @Override
  protected boolean isRelevantRequestType(JsonNode jsonRequest) {
    return JsonToolkit.stringAttributeEquals(jsonRequest, "action", accept ? "SURR" : "SREJ");
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return hasChildren() || EblSurrenderV10Role.isPlatform(roleName);
  }
}
