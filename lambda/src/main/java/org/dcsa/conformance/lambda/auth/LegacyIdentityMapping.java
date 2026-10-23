package org.dcsa.conformance.lambda.auth;

/**
 * A prepared legacy lookup entry. Blocked entries must never grant access to an environment.
 */
public record LegacyIdentityMapping(String environmentId, boolean blocked) {
}
