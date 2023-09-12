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
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegrationProps;
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

    HttpApi httpApi =
        new HttpApi(
            this,
            "conformance-gateway-api",
            HttpApiProps.builder().apiName("conformance-gateway-api").build());

    httpApi.addRoutes(
        AddRoutesOptions.builder()
            .path("/conformance")
            .methods(singletonList(HttpMethod.GET))
            .integration(
                new HttpLambdaIntegration(
                    "ConformanceGatewayIntegration",
                    conformanceLambda,
                    HttpLambdaIntegrationProps.builder()
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
            .build());

    new CfnOutput(
        this,
        "HttApi",
        CfnOutputProps.builder()
            .description("Url for Http Api")
            .value(httpApi.getApiEndpoint())
            .build());
  }
}
