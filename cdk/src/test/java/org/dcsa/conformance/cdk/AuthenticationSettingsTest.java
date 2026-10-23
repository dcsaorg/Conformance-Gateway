package org.dcsa.conformance.cdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.assertions.Template;

class AuthenticationSettingsTest {
  static Map<String, Object> auth0(String audience, String client) {
    return Map.of("issuer", "https://tenant.example/", "audience", audience, "clientId", client);
  }

  @Test
  void auth0SettingsMustBeComplete() {
    for (String deployment : List.of("dt", "dev", "test")) {
      assertEquals(
          deployment + "-api",
          AuthenticationSettings.fromContext(auth0(deployment + "-api", deployment + "-spa"))
              .audience());
      assertThrows(IllegalArgumentException.class, () -> AuthenticationSettings.fromContext(null));
      assertThrows(
          IllegalArgumentException.class, () -> AuthenticationSettings.fromContext(Map.of()));
      assertThrows(
          IllegalArgumentException.class,
          () -> AuthenticationSettings.fromContext(Map.of("provider", "cognito")));
    }
    assertThrows(
        IllegalArgumentException.class, () -> new AuthenticationSettings(null, "api", "spa"));
    for (String issuer :
        List.of(
            "http://tenant.example/",
            "https://tenant.example/path/",
            "https://tenant.example/?x=y",
            "https://user@tenant.example/")) {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              AuthenticationSettings.fromContext(
                  Map.of("issuer", issuer, "audience", "api", "clientId", "spa")));
    }
    assertThrows(
        IllegalArgumentException.class,
        () -> AuthenticationSettings.fromContext(auth0("same", "same")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AuthenticationSettings("http://tenant.example/", "api", "spa"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AuthenticationSettings("https://tenant.example/", "api", " "));
  }

  private static Map<String, AuthenticationSettings> configured(Map<String, Object> context) {
    return ConformanceApp.configuredDeployments(
        new App(AppProps.builder().context(context).build()));
  }

  @Test
  void onlyConfiguredEnvironmentsAreSynthesized() {
    assertThrows(IllegalArgumentException.class, () -> configured(Map.of()));
    var context = Map.<String, Object>of("dtAuth", auth0("dt-api", "dt-spa"));
    assertEquals(java.util.Set.of("dt"), configured(context).keySet());
    assertThrows(IllegalArgumentException.class, () -> configured(Map.of("dtAuth", Map.of())));
  }

  @Test
  void sharedStagingTenantStillRequiresDistinctAudiencesAndClients() {
    var dt = auth0("dt-api", "dt-spa");
    var dev = auth0("dev-api", "dev-spa");
    assertEquals(
        java.util.Set.of("dt", "dev"), configured(Map.of("dtAuth", dt, "devAuth", dev)).keySet());
    assertThrows(
        IllegalArgumentException.class,
        () -> configured(Map.of("dtAuth", dt, "devAuth", auth0("dt-api", "dev-spa"))));
    assertThrows(
        IllegalArgumentException.class,
        () -> configured(Map.of("dtAuth", dt, "devAuth", auth0("dev-api", "dt-spa"))));
  }

  @Test
  void auth0IsTheOnlyAuthorizerAndLegacyResourcesKeepTheirOriginalScope() {
    // Disable asset bundling: template contracts need no Docker, AWS credentials, or network
    // calls.
    App app =
        new App(AppProps.builder().context(Map.of("aws:cdk:bundling-stacks", List.of())).build());
    Stack stack =
        new ConformanceStack(
            app,
            StackProps.builder()
                .env(Environment.builder().account("123456789012").region("eu-north-1").build())
                .build(),
            "FixtureStack",
            "dt",
            "ZFIXTURE",
            "example.com",
            "arn:aws:lambda:eu-north-1:123456789012:layer:insights:1",
            "arn:aws:acm:eu-north-1:123456789012:certificate/fixture",
            "arn:aws:acm:eu-north-1:123456789012:certificate/fixture",
            "arn:aws:acm:us-east-1:123456789012:certificate/fixture",
            false,
            AuthenticationSettings.fromContext(auth0("dt-api", "dt-spa")));
    Template template = Template.fromStack(stack);
    assertTrue(stack.getNode().tryFindChild("dtUserPool") != null);
    assertTrue(
        stack.getNode().findChild("dtUserPool").getNode().tryFindChild("dtUserPoolClient") != null);
    template.resourceCountIs("AWS::Cognito::UserPool", 1);
    template.resourceCountIs("AWS::Cognito::UserPoolClient", 1);
    template.resourceCountIs("AWS::ApiGatewayV2::Authorizer", 1);
    template.resourceCountIs("AWS::ElasticLoadBalancingV2::LoadBalancer", 1);
    template.hasResourceProperties(
        "AWS::ApiGatewayV2::Authorizer", Map.of("AuthorizerType", "JWT"));
    template.hasResourceProperties(
        "AWS::ApiGatewayV2::Route",
        Map.of("RouteKey", "POST /conformance/webui", "AuthorizationType", "JWT"));
    for (String type : List.of("AWS::Cognito::UserPool", "AWS::Cognito::UserPoolClient")) {
      template
          .findResources(type)
          .values()
          .forEach(
              resource -> {
                assertEquals("Retain", resource.get("DeletionPolicy"));
                assertEquals("Retain", resource.get("UpdateReplacePolicy"));
              });
    }
    template.hasResourceProperties(
        "AWS::ApiGatewayV2::Authorizer",
        Map.of(
            "JwtConfiguration",
            Map.of("Issuer", "https://tenant.example/", "Audience", List.of("dt-api"))));
    template.resourceCountIs("AWS::KMS::Key", 1);
    template.hasResourceProperties(
        "AWS::Lambda::Function",
        Map.of(
            "FunctionName",
            "dtWebuiLambda",
            "Environment",
            Map.of(
                "Variables",
                Map.of(
                    "AUTH_ISSUER",
                    "https://tenant.example/",
                    "AUTH_AUDIENCE",
                    "dt-api",
                    "AUTH_CLIENT_ID",
                    "dt-spa"))));
    assertFalse(template.toJSON().toString().contains("AUTH_PROVIDER"));
    assertFalse(template.toJSON().toString().contains("cognito-idp."));
  }
}
