package org.dcsa.conformance.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public final class ConformanceApp {
  public static final String REGION = "eu-north-1";

  public static void main(final String[] args) {
    App app = new App();

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
        true);

    new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region(REGION).build()).build(),
        "dtConformanceStack",
        "dt",
        "Z08637291XUXSIEGQ5KNR",
        "conformance-dt-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        // dt-api.conformance-dt-1.dcsa.org
        "arn:aws:acm:eu-north-1:231663969095:certificate/5b911671-b1b1-4435-9d68-654828be4ac0",
        // dt-webui.conformance-dt-1.dcsa.org
        "arn:aws:acm:eu-north-1:231663969095:certificate/58332d65-4f49-4216-9ebc-d8ed2219abdf",
        // dt.conformance-dt-1.dcsa.org
        "arn:aws:acm:us-east-1:231663969095:certificate/bba79568-0523-4b34-9844-accedcb4bd5e",
        false);

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
        true);

    app.synth();
  }
}
