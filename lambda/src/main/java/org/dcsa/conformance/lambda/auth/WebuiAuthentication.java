package org.dcsa.conformance.lambda.auth;

import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kms.KmsClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.BiFunction;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public final class WebuiAuthentication {

  public static final String PERMISSION = "standards:functionality:test-conformance";

  private WebuiAuthentication() {
  }

  public static WebuiIdentity fromEnvironment() {
    Map<String, String> env = System.getenv();
    String issuer = required(env, "AUTH_ISSUER");
    String audience = required(env, "AUTH_AUDIENCE");
    String clientId = required(env, "AUTH_CLIENT_ID");
    String tableName = required(env, "TABLE_NAME");
    String hmacKeyArn = required(env, "IDENTITY_HMAC_KEY_ARN");
    var timeout = ClientOverrideConfiguration.builder()
        .apiCallTimeout(Duration.ofSeconds(10))
        .apiCallAttemptTimeout(Duration.ofSeconds(4))
        .build();
    var mappings = new DynamoIdentityMappingRepository(
        DynamoDbClient.builder().overrideConfiguration(timeout).build(), tableName, hmacKeyArn);
    var emailHasher = new KmsEmailHasher(KmsClient.builder().overrideConfiguration(timeout).build(), hmacKeyArn);
    var userInfo = new Auth0UserInfoFetcher(issuer, userInfoHttpClient());
    var resolver = new Auth0IdentityResolver(issuer, mappings, userInfo::fetch, emailHasher);
    return auth0(issuer, audience, clientId, resolver::resolve);
  }

  static HttpClient userInfoHttpClient() {
    return HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
  }

  /**
   * Signature, issuer, audience, exp/nbf/iat validation is performed by the native JWT authorizer.
   * These additional checks fail closed on misconfiguration; they are not a JWT validator.
   */
  public static WebuiIdentity auth0(
    String issuer, String audience, String clientId, BiFunction<String, String, String> resolve) {
    return (claims, token) -> {
      if (!issuer.equals(claims.path("iss").asText())
        || !contains(claims.path("aud"), audience)
        || !clientId.equals(claims.path("azp").asText())
        || claims.path("sub").asText().isBlank()
        || claims.path("exp").asLong() <= Instant.now().getEpochSecond()
        || token == null
        || !token.startsWith("Bearer ")
        || token.substring(7).isBlank()) {
        throw new WebuiIdentity.Failure(401, "Authentication required");
      }
      if (!claims.path("org_id").asText().matches("org_[A-Za-z0-9]+")
        || !contains(claims.path("permissions"), PERMISSION)) {
        throw new WebuiIdentity.Failure(403, "Conformance access required");
      }
      return resolve.apply(claims.path("sub").asText(), token.substring(7));
    };
  }

  private static boolean contains(JsonNode claim, String expected) {
    if (claim.isTextual()) {
      if (expected.equals(claim.asText())) return true;
      try {
        claim = OBJECT_MAPPER.readTree(claim.asText());
      } catch (Exception e) {
        return false;
      }
    }
    if (!claim.isArray()) return false;
    for (JsonNode value : claim)
      if (value.isTextual() && expected.equals(value.asText())) return true;
    return false;
  }

  private static String required(Map<String, String> env, String key) {
    String value = env.get(key);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
    return value;
  }
}
