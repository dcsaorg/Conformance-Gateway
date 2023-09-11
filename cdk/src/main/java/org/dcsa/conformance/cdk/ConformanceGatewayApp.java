package org.dcsa.conformance.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public final class ConformanceGatewayApp {
  public static void main(final String[] args) {
    App app = new App();

    new ConformanceGatewayStack(
        app,
        "ConformanceGatewayStack",
        StackProps.builder().env(Environment.builder().region("eu-north-1").build()).build());

    app.synth();
  }
}
