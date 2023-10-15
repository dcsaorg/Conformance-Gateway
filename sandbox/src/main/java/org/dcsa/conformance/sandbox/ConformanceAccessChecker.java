package org.dcsa.conformance.sandbox;

public interface ConformanceAccessChecker {
    String getUserEnvironmentId(String userId);
    void checkUserSandboxAccess(String userId, String sandboxId);
}
