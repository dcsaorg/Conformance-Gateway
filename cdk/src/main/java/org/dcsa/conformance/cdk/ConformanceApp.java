package org.dcsa.conformance.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConformanceApp {
  public static final String REGION = "eu-north-1";

  static void main(final String[] ignoredArgs) {
    App app = new App();

    var authentication = configuredDeployments(app);

    if (authentication.containsKey("dev"))
      new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region(REGION).build()).build(),
        "devConformanceStack",
        "dev",
        "Z0720499I10UDAXMT620",
        "conformance-development-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        "arn:aws:acm:eu-north-1:468100668426:certificate/a6ea21c7-a709-406d-b0df-a3c3b0120672",
        "arn:aws:acm:eu-north-1:468100668426:certificate/00bd8fef-16a9-4c4e-800a-ea4e61ffc24e",
        "arn:aws:acm:us-east-1:468100668426:certificate/b3d816d1-8224-4256-85f5-057e2a3da9e7",
        false,
        authentication.get("dev"));

    if (authentication.containsKey("dt"))
      new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region(REGION).build()).build(),
        "dtConformanceStack",
        "dt",
        "Z08637291XUXSIEGQ5KNR",
        "conformance-dt-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        "arn:aws:acm:eu-north-1:231663969095:certificate/5b911671-b1b1-4435-9d68-654828be4ac0",
        "arn:aws:acm:eu-north-1:231663969095:certificate/58332d65-4f49-4216-9ebc-d8ed2219abdf",
        "arn:aws:acm:us-east-1:231663969095:certificate/bba79568-0523-4b34-9844-accedcb4bd5e",
        false,
        authentication.get("dt"));

    if (authentication.containsKey("test"))
      new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region(REGION).build()).build(),
        "testConformanceStack",
        "test",
        "Z00173773E1E6BI3P1ZF0",
        "conformance-test-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        "arn:aws:acm:eu-north-1:346080125434:certificate/ac55dc0e-e348-4493-b6fc-19e52e6d2438",
        "arn:aws:acm:eu-north-1:346080125434:certificate/bd53d408-5da2-494c-9eae-b33787823915",
        "arn:aws:acm:us-east-1:346080125434:certificate/f68b8c56-20b8-40d1-9a1f-ce8c2ac14a2a",
        true,
        authentication.get("test"));

    app.synth();
  }

  /**
   * Only configured environments participate in this synthesis
   */
  static Map<String, AuthenticationSettings> configuredDeployments(App app) {
    Map<String, AuthenticationSettings> deployments = new LinkedHashMap<>();
    for (String deployment : List.of("dt", "dev", "test")) {
      Object context = app.getNode().tryGetContext(deployment + "Auth");
      deployments.put(deployment, AuthenticationSettings.fromContext(context));
    }
    var audiences = new HashSet<String>();
    var clients = new HashSet<List<String>>();
    for (AuthenticationSettings auth : deployments.values()) {
      if (!audiences.add(auth.audience()) || !clients.add(List.of(auth.issuer(), auth.clientId()))) {
        throw new IllegalArgumentException("Auth0 deployments require distinct audiences and SPA registrations");
      }
    }
    return Map.copyOf(deployments);
  }
}
