package org.dcsa.conformance.cdk;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import java.util.Arrays;
import java.util.List;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

public class ConformanceGatewayStack extends Stack {
  public ConformanceGatewayStack(final Construct parent, final String id, final StackProps props) {
    super(parent, id, props);

    List<String> packagingInstructions =
        Arrays.asList(
            "/bin/sh",
            "-c",
            "mvn clean install && cp /asset-input/target/conformance-lambda.jar /asset-output/");

    BundlingOptions.Builder builderOptions =
        BundlingOptions.builder()
            .command(packagingInstructions)
            .image(Runtime.JAVA_17.getBundlingImage())
            .volumes(
                singletonList(
                    // Mount local .m2 repo to avoid download all the dependencies again inside the
                    // container
                    DockerVolume.builder()
                        .hostPath(System.getProperty("user.home") + "/.m2/")
                        .containerPath("/root/.m2/")
                        .build()))
            .user("root")
            .outputType(ARCHIVED);

    Function conformanceLambda =
        new Function(
            this,
            "ConformanceGatewayLambda",
            FunctionProps.builder()
                .runtime(Runtime.JAVA_17)
                .code(
                    Code.fromAsset(
                        "../lambda/",
                        AssetOptions.builder().bundling(builderOptions.build()).build()))
                .handler("org.dcsa.conformance.lambda.ConformanceSandboxLambda")
                .memorySize(1024)
                .timeout(Duration.minutes(15))
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());

    LambdaRestApi lambdaRestApi =
        new LambdaRestApi(
            this,
            "conformance-lambda-rest-api",
            LambdaRestApiProps.builder()
                .restApiName("conformance-lambda-rest-api")
                .handler(conformanceLambda)
                .proxy(true)
                .build());

    new CfnOutput(
        this,
        "lambda-rest-api-url",
        CfnOutputProps.builder().description("RestAPI URL").value(lambdaRestApi.getUrl()).build());
  }
}
