package org.dcsa.conformance.lambda.auth;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class Auth0IdentityResolver {
  private final String issuer;
  private final IdentityMappingRepository mappings;
  private final Function<String, JsonNode> userInfo;
  private final UnaryOperator<String> emailHasher;

  public Auth0IdentityResolver(
    String issuer,
    IdentityMappingRepository mappings,
    Function<String, JsonNode> userInfo,
    UnaryOperator<String> emailHasher) {
    this.issuer = issuer;
    this.mappings = mappings;
    this.userInfo = userInfo;
    this.emailHasher = emailHasher;
  }

  public String resolve(String subject, String accessToken) {
    String identity = identityKey(issuer, subject);
    String existing = mappings.find(identity);
    if (existing != null) return existing;
    // A complete, explicitly activated inventory is required even for genuinely new users.
    String version = mappings.migrationVersion();
    JsonNode profile = userInfo.apply(accessToken);
    if (!subject.equals(profile.path("sub").asText())
      || !profile.path("email_verified").isBoolean()
      || !profile.path("email_verified").booleanValue()) {
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support");
    }
    String emailKey = version + "#" + emailHasher.apply(normalizeEmail(profile.path("email").asText()));
    LegacyIdentityMapping legacy = mappings.legacy(emailKey);
    if (legacy != null && legacy.blocked()) {
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support");
    }
    String environmentId = legacy == null ? "auth0-" + UUID.randomUUID() : legacy.environmentId();
    return mappings.link(identity, environmentId, emailKey, legacy, version);
  }

  /**
   * ASCII trim + lower-case, shared with migration tooling; no provider-specific alias folding.
   */
  public static String normalizeEmail(String email) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[\\x21-\\x7e]+@[\\x21-\\x7e]+\\.[\\x21-\\x7e]+")
      || normalized.indexOf('@') != normalized.lastIndexOf('@')
      || normalized.length() > 254) {
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support");
    }
    return normalized;
  }

  public static String identityKey(String issuer, String subject) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest((issuer + "\n" + subject).getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
