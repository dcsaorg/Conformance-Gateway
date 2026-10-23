package org.dcsa.conformance.lambda.auth;

/**
 * Persistence boundary for personal workspace ownership; independent of Auth0 HTTP and KMS.
 */
public interface IdentityMappingRepository {
  /**
   * Returns the active environment ID, or null when unmapped; inconsistent/revoked owners fail.
   */
  String find(String identity);

  /**
   * Returns the activated inventory version; fails when preparation is incomplete.
   */
  String migrationVersion();

  /**
   * Looks up a version-prefixed email digest; null means no legacy account matched.
   */
  LegacyIdentityMapping legacy(String emailKey);

  /**
   * Atomically claims an unowned environment and checks that the inventory/lookup still match.
   * Concurrent calls for this identity reuse its winner; another identity's ownership is never
   * replaced.
   */
  String link(
    String identity,
    String environmentId,
    String emailKey,
    LegacyIdentityMapping legacy,
    String version);
}
