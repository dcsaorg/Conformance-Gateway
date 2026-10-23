package org.dcsa.conformance.lambda.auth;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Map;
import java.util.function.BiFunction;
import org.dcsa.conformance.lambda.WebuiLambda;
import org.junit.jupiter.api.Test;

class WebuiAuthenticationTest {
  private static final String ISSUER = "https://tenant.example/";

  private static APIGatewayProxyRequestEvent admissionRequest(ObjectNode claims) {
    var request =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"operation\":\"checkAccess\"}")
            .withHeaders(Map.of("authorization", "Bearer token"));
    var context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
    context.setAuthorizer(
        Map.of("jwt", Map.of("claims", OBJECT_MAPPER.convertValue(claims, Map.class))));
    request.setRequestContext(context);
    return request;
  }

  @Test
  @SuppressWarnings("unchecked")
  void admissionProbeSucceedsOnlyAfterResolvingPersonalOwnership() {
    BiFunction<String, String, String> resolver = mock(BiFunction.class);
    when(resolver.apply("auth0|user", "token")).thenReturn("legacy-environment");
    var auth = WebuiAuthentication.auth0(ISSUER, "dt-api", "dt-client", resolver);
    var result = new WebuiLambda(auth).handleRequest(admissionRequest(claims()), null);
    assertEquals(200, result.getStatusCode());
    assertEquals("{}", result.getBody());
    assertEquals("no-store", result.getHeaders().get("Cache-Control"));
    verify(resolver).apply("auth0|user", "token");
  }

  @Test
  void admissionProbeDoesNotBypassPermissionOrIdentityFailures() {
    var unauthorized = claims();
    unauthorized.remove("permissions");
    var auth =
        WebuiAuthentication.auth0(
            ISSUER, "dt-api", "dt-client", (s, t) -> fail("must not resolve"));
    assertEquals(
        403,
        new WebuiLambda(auth).handleRequest(admissionRequest(unauthorized), null).getStatusCode());
    for (int status : new int[] {409, 503}) {
      var failingAuth =
          WebuiAuthentication.auth0(
              ISSUER,
              "dt-api",
              "dt-client",
              (s, t) -> {
                throw new WebuiIdentity.Failure(status, "Identity unavailable");
              });
      assertEquals(
          status,
          new WebuiLambda(failingAuth)
              .handleRequest(admissionRequest(claims()), null)
              .getStatusCode());
    }
  }

  static ObjectNode claims() {
    var claims =
        OBJECT_MAPPER
            .createObjectNode()
            .put("iss", ISSUER)
            .put("azp", "dt-client")
            .put("sub", "auth0|user")
            .put("org_id", "org_one")
            .put("exp", Instant.now().getEpochSecond() + 120);
    claims.putArray("aud").add("dt-api").add(ISSUER + "userinfo");
    claims.putArray("permissions").add(WebuiAuthentication.PERMISSION);
    return claims;
  }

  @Test
  void authorizesOnlyDeploymentPermissionAndKeepsOrganizationOutOfOwnership() {
    var auth =
        WebuiAuthentication.auth0(
            ISSUER, "dt-api", "dt-client", (sub, token) -> "legacy-environment");
    assertEquals("legacy-environment", auth.resolve(claims(), "Bearer token"));
    assertEquals(
        "legacy-environment", auth.resolve(claims().put("org_id", "org_two"), "Bearer token"));
    assertEquals(
        "legacy-environment",
        auth.resolve(
            claims()
                .put("aud", "[\"dt-api\",\"https://tenant.example/userinfo\"]")
                .put("permissions", "[\"" + WebuiAuthentication.PERMISSION + "\"]"),
            "Bearer token"));
  }

  @Test
  void rejectsMissingPermissionAndOrgBeforeIdentityLookup() {
    @SuppressWarnings("unchecked")
    BiFunction<String, String, String> resolver = mock(BiFunction.class);
    var auth = WebuiAuthentication.auth0(ISSUER, "dt-api", "dt-client", resolver);
    for (String field : new String[] {"permissions", "org_id"}) {
      var claims = claims();
      claims.remove(field);
      assertEquals(
          403,
          assertThrows(WebuiIdentity.Failure.class, () -> auth.resolve(claims, "Bearer token"))
              .status());
    }
    assertEquals(
        403,
        assertThrows(
                WebuiIdentity.Failure.class,
                () ->
                    auth.resolve(
                        claims().put("permissions", "standards:functionality:all-functionalities"),
                        "Bearer token"))
            .status());
    assertEquals(
        403,
        assertThrows(
                WebuiIdentity.Failure.class,
                () -> auth.resolve(claims().put("org_id", "org_one,org_two"), "Bearer token"))
            .status());
    verifyNoInteractions(resolver);
  }

  @Test
  void rejectsExpiredWrongIssuerAudienceClientAndMissingSubject() {
    var auth =
        WebuiAuthentication.auth0(
            ISSUER, "dt-api", "dt-client", (s, t) -> fail("must not resolve"));
    for (ObjectNode invalid :
        new ObjectNode[] {
          claims().put("iss", "https://other.example/"),
          claims().put("aud", "dev-api"),
          claims().put("azp", "dev-client"),
          claims().put("sub", ""),
          claims().put("exp", 1),
          claims().put("aud", "dt-client")
        }) {
      assertEquals(
          401,
          assertThrows(WebuiIdentity.Failure.class, () -> auth.resolve(invalid, "Bearer token"))
              .status());
    }
    assertEquals(
        401,
        assertThrows(WebuiIdentity.Failure.class, () -> auth.resolve(claims(), null)).status());
  }

  @Test
  void rejectsLegacyCredentialsWithoutResolvingOwnership() {
    var auth =
        WebuiAuthentication.auth0(
            ISSUER, "dt-api", "dt-client", (s, t) -> fail("must not resolve"));
    var legacy =
        OBJECT_MAPPER
            .createObjectNode()
            .put("iss", "https://cognito.example/pool")
            .put("cognito:username", "original");
    assertEquals(
        401,
        assertThrows(WebuiIdentity.Failure.class, () -> auth.resolve(legacy, "Bearer legacy-token"))
            .status());
  }

  @Test
  void directApiWithoutValidatedContextIsUnauthorized() {
    var identity = mock(WebuiIdentity.class);
    var result =
        new WebuiLambda(identity)
            .handleRequest(
                new APIGatewayProxyRequestEvent()
                    .withHeaders(Map.of("Authorization", "Bearer forged"))
                    .withBody(
                        "{\"environmentId\":\"victim\",\"permissions\":[\""
                            + WebuiAuthentication.PERMISSION
                            + "\"]}"),
                null);
    assertEquals(401, result.getStatusCode());
    verifyNoInteractions(identity);
  }

  @Test
  void directApiWithoutPermissionIsForbiddenBeforeDatabaseAccess() {
    var request =
        new APIGatewayProxyRequestEvent()
            .withBody("{\"operation\":\"getAllSandboxes\"}")
            .withHeaders(Map.of("authorization", "Bearer token"));
    var context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
    var claims = claims();
    claims.remove("permissions");
    context.setAuthorizer(
        Map.of("jwt", Map.of("claims", OBJECT_MAPPER.convertValue(claims, Map.class))));
    request.setRequestContext(context);
    var auth =
        WebuiAuthentication.auth0(
            ISSUER, "dt-api", "dt-client", (s, t) -> fail("must not resolve"));
    assertEquals(403, new WebuiLambda(auth).handleRequest(request, null).getStatusCode());
  }
}
