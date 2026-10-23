package org.dcsa.conformance.lambda.auth;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Auth0IdentityResolverTest {

  static class MemoryMappings implements IdentityMappingRepository {

    final Map<String, String> identities = new ConcurrentHashMap<>();
    final Map<String, String> owners = new ConcurrentHashMap<>();
    final Map<String, LegacyIdentityMapping> legacy = new ConcurrentHashMap<>();

    boolean ready = true;

    public String find(String id) {
      return identities.get(id);
    }

    public String migrationVersion() {
      if (!ready) throw new WebuiIdentity.Failure(503, "not ready");
      return "version";
    }

    public LegacyIdentityMapping legacy(String email) {
      return legacy.get(email);
    }

    public synchronized String link(
      String id, String environment, String email, LegacyIdentityMapping legacy, String version) {
      if (identities.containsKey(id)) return identities.get(id);
      if (owners.containsKey(environment)) throw new WebuiIdentity.Failure(409, "conflict");
      owners.put(environment, id);
      identities.put(id, environment);
      return environment;
    }
  }

  static JsonNode profile(String subject, boolean verified) {
    return OBJECT_MAPPER
      .createObjectNode()
      .put("sub", subject)
      .put("email", " Alice@Example.COM ")
      .put("email_verified", verified);
  }

  @Test
  void existingUserKeepsLegacyEnvironmentAndOnlyLooksUpEmailOnce() {
    var mappings = new MemoryMappings();
    mappings.legacy.put(
      "version#alice@example.com", new LegacyIdentityMapping("cognito-username", false));
    var calls = new AtomicInteger();
    var hashes = new AtomicInteger();
    var resolver =
      new Auth0IdentityResolver(
        "https://issuer.example/",
        mappings,
        token -> {
          calls.incrementAndGet();
          return profile("auth0|alice", true);
        },
        email -> {
          hashes.incrementAndGet();
          return email;
        });
    assertEquals("cognito-username", resolver.resolve("auth0|alice", "token"));
    assertEquals("cognito-username", resolver.resolve("auth0|alice", "renewed-token"));
    assertEquals(1, calls.get());
    assertEquals(1, hashes.get());
  }

  @Test
  void genuinelyNewUsersGetStableApplicationOwnedIds() {
    var mappings = new MemoryMappings();
    var resolver =
      new Auth0IdentityResolver(
        "https://issuer.example/", mappings, t -> profile("new", true), email -> email);
    String id = resolver.resolve("new", "token");
    assertTrue(id.startsWith("auth0-"));
    assertEquals(id, resolver.resolve("new", "token"));
  }

  @Test
  void missingInventoryDisabledAmbiguousUnverifiedAndMismatchedIdentitiesFailClosed() {
    var mappings = new MemoryMappings();
    var resolver =
      new Auth0IdentityResolver(
        "https://issuer.example/", mappings, t -> profile("user", true), email -> email);
    mappings.ready = false;
    assertEquals(
      503,
      assertThrows(WebuiIdentity.Failure.class, () -> resolver.resolve("user", "token"))
        .status());
    mappings.ready = true;
    mappings.legacy.put("version#alice@example.com", new LegacyIdentityMapping("", true));
    assertThrows(WebuiIdentity.Failure.class, () -> resolver.resolve("user", "token"));
    mappings.legacy.clear();
    for (JsonNode profile :
      new JsonNode[]{
        profile("other-user", true),
        profile("user", false),
        OBJECT_MAPPER.createObjectNode().put("sub", "user").put("email_verified", "true")
      }) {
      var invalid =
        new Auth0IdentityResolver(
          "https://issuer.example/", mappings, t -> profile, email -> email);
      assertThrows(WebuiIdentity.Failure.class, () -> invalid.resolve("user", "token"));
    }
    assertTrue(mappings.identities.isEmpty());
  }

  @Test
  void emailDoesNotAllowASecondIdentityToTakeOverLegacyData() {
    var mappings = new MemoryMappings();
    mappings.legacy.put("version#alice@example.com", new LegacyIdentityMapping("legacy", false));
    var resolver =
      new Auth0IdentityResolver(
        "https://issuer.example/", mappings, t -> profile(t, true), email -> email);
    assertEquals("legacy", resolver.resolve("first", "first"));
    assertThrows(WebuiIdentity.Failure.class, () -> resolver.resolve("second", "second"));
  }

  @Test
  void concurrentFirstLoginsAreIdempotent() throws Exception {
    var mappings = new MemoryMappings();
    var resolver =
      new Auth0IdentityResolver(
        "https://issuer.example/", mappings, t -> profile("new", true), email -> email);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var results =
        executor.invokeAll(
          IntStream.range(0, 30)
            .mapToObj(i -> (Callable<String>) () -> resolver.resolve("new", "token"))
            .toList());
      String winner = results.getFirst().get();
      for (var result : results) assertEquals(winner, result.get());
    }
    assertEquals(1, mappings.identities.size());
    assertEquals(1, mappings.owners.size());
  }

  @Test
  void deploymentDataAndIssuerNamespacesRemainSeparate() {
    var dt = new MemoryMappings();
    var dev = new MemoryMappings();
    dt.legacy.put("version#alice@example.com", new LegacyIdentityMapping("dt-legacy", false));
    dev.legacy.put("version#alice@example.com", new LegacyIdentityMapping("dev-legacy", false));
    assertEquals(
      "dt-legacy",
      new Auth0IdentityResolver(
        "https://staging.example/", dt, t -> profile("alice", true), email -> email)
        .resolve("alice", "token"));
    assertEquals(
      "dev-legacy",
      new Auth0IdentityResolver(
        "https://staging.example/", dev, t -> profile("alice", true), email -> email)
        .resolve("alice", "token"));
    assertNotEquals(
      Auth0IdentityResolver.identityKey("https://one.example/", "alice"),
      Auth0IdentityResolver.identityKey("https://two.example/", "alice"));
  }

  @Test
  void normalizationDoesNotInventAliasesAndRejectsUnsupportedEmail() {
    assertEquals(
      "alice+tag@example.com", Auth0IdentityResolver.normalizeEmail(" Alice+Tag@EXAMPLE.COM "));
    for (String email :
      new String[]{"", "alice", "a@@example.com", "a@exämple.com", "a b@example.com"}) {
      assertThrows(WebuiIdentity.Failure.class, () -> Auth0IdentityResolver.normalizeEmail(email));
    }
  }
}
