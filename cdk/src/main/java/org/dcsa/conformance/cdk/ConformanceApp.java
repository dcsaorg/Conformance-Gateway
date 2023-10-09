package org.dcsa.conformance.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public final class ConformanceApp {
  public static void main(final String[] args) {
    App app = new App();

    new ConformanceStack(
        app,
        StackProps.builder().env(Environment.builder().region("eu-north-1").build()).build(),
        "dev",
        "Z0720499I10UDAXMT620",
        "conformance-development-1.dcsa.org",
        "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35",
        "arn:aws:acm:eu-north-1:468100668426:certificate/a6ea21c7-a709-406d-b0df-a3c3b0120672",
        "arn:aws:acm:eu-north-1:468100668426:certificate/00bd8fef-16a9-4c4e-800a-ea4e61ffc24e",
        "arn:aws:acm:us-east-1:468100668426:certificate/b3d816d1-8224-4256-85f5-057e2a3da9e7");

    app.synth();
  }
}
