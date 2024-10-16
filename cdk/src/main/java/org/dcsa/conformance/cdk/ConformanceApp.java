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
        "arn:aws:acm:us-east-1:468100668426:certificate/b3d816d1-8224-4256-85f5-057e2a3da9e7");

    new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region(REGION).build()).build(),
        "devtestConformanceStack",
        "test", // in the dev account
        "Z0720499I10UDAXMT620",
        "conformance-development-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        "arn:aws:acm:eu-north-1:468100668426:certificate/64b2b920-f435-4ebc-9874-eb97e6bd9bf9",
        "arn:aws:acm:eu-north-1:468100668426:certificate/607709d1-0910-43d1-adb8-9b2fc81cfa60",
        "arn:aws:acm:us-east-1:468100668426:certificate/3ae61e03-9e88-4d6c-a3ac-9ac2943d80e1");

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
        "arn:aws:acm:us-east-1:346080125434:certificate/f68b8c56-20b8-40d1-9a1f-ce8c2ac14a2a");

    app.synth();
  }
}
