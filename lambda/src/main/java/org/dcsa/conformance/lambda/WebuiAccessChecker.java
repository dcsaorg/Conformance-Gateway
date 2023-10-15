package org.dcsa.conformance.lambda;

import org.dcsa.conformance.sandbox.ConformanceAccessChecker;
import org.dcsa.conformance.sandbox.ConformanceAccessException;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

public class WebuiAccessChecker implements ConformanceAccessChecker {
  private final ConformancePersistenceProvider persistenceProvider;

  public WebuiAccessChecker(ConformancePersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  @Override
  public String getUserEnvironmentId(String userId) {
    return userId;
  }

  @Override
  public void checkUserSandboxAccess(String userId, String sandboxId) {
    if (!persistenceProvider
        .getNonLockingMap()
        .getItemValue("environment#" + userId, "sandbox#" + sandboxId)
        .has("id")) {
      throw new ConformanceAccessException(
          "User '%s' has no access to sandbox '%s'".formatted(userId, sandboxId));
    }
  }
}
