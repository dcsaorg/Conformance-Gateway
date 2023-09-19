package org.dcsa.conformance.cdk;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;

public class ConformanceGatewayStack extends Stack {
  public ConformanceGatewayStack(final Construct parent, final String id, final StackProps props) {
    super(parent, id, props);

    Table table =
        new Table(
            this,
            "conformance-table",
            TableProps.builder()
                .tableName("conformance")
                .partitionKey(Attribute.builder().name("PK").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("SK").type(AttributeType.STRING).build())
                .pointInTimeRecovery(true)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

    AssetCode assetCode =
        Code.fromAsset(
            "../lambda/",
            AssetOptions.builder()
                .bundling(
                    BundlingOptions.builder()
                        .command(
                            Arrays.asList(
                                "/bin/sh",
                                "-c",
                                "mvn clean install && cp /asset-input/target/conformance-lambda.jar /asset-output/"))
                        .image(Runtime.JAVA_17.getBundlingImage())
                        .volumes(
                            singletonList(
                                // Mount local .m2 repo to avoid download all the
                                // dependencies again inside the container
                                DockerVolume.builder()
                                    .hostPath(System.getProperty("user.home") + "/.m2/")
                                    .containerPath("/root/.m2/")
                                    .build()))
                        .user("root")
                        .outputType(ARCHIVED)
                        .build())
                .build());

    Function httpClientLambda =
        new Function(
            this,
            "HttpClientLambda",
            FunctionProps.builder()
                .functionName("HttpClientLambda")
                .runtime(Runtime.JAVA_17)
                .code(assetCode)
                .handler("org.dcsa.conformance.lambda.HttpClientLambda")
                .memorySize(1024)
                .timeout(Duration.minutes(1))
                .reservedConcurrentExecutions(16)
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());

    Function conformanceLambda =
        new Function(
            this,
            "ConformanceSandboxLambda",
            FunctionProps.builder()
                .functionName("ConformanceSandboxLambda")
                .runtime(Runtime.JAVA_17)
                .code(assetCode)
                .handler("org.dcsa.conformance.lambda.ConformanceSandboxLambda")
                .memorySize(1024)
                .timeout(Duration.minutes(1))
                .reservedConcurrentExecutions(16)
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());
    table.grantReadWriteData(conformanceLambda);

    Objects.requireNonNull(conformanceLambda.getRole())
        .attachInlinePolicy(
            new Policy(
                this,
                "CanInvokeHttpClientLambdaPolicy",
                PolicyProps.builder()
                    .statements(
                        List.of(
                            PolicyStatement.Builder.create()
                                .actions(List.of("lambda:InvokeFunction"))
                                .resources(List.of(httpClientLambda.getFunctionArn()))
                                .build()))
                    .build()));

    Stream.of(httpClientLambda, conformanceLambda)
        .forEach(
            lambda -> {
              Objects.requireNonNull(lambda.getRole())
                  .addManagedPolicy(
                      ManagedPolicy.fromAwsManagedPolicyName(
                          "CloudWatchLambdaInsightsExecutionRolePolicy"));
              lambda.addLayers(
                  LayerVersion.fromLayerVersionArn(
                      this,
                      lambda.getFunctionName() + "-insights-layer",
                      "arn:aws:lambda:eu-north-1:580247275435:layer:LambdaInsightsExtension:35"));
            });

    // TODO
    //    AccessLogFormat.custom(
    //        new ObjectMapper()
    //            .createObjectNode()
    //            .put("contextApiId", AccessLogField.contextApiId())
    // ...
    //            .toString());
    LambdaRestApi lambdaRestApi =
        new LambdaRestApi(
            this,
            "conformance-lambda-rest-api",
            LambdaRestApiProps.builder()
                .restApiName("conformance-lambda-rest-api")
                .handler(conformanceLambda)
                .proxy(true)
                .deployOptions(
                    StageOptions.builder()
                        .accessLogDestination(
                            new LogGroupLogDestination(
                                new LogGroup(this, "conformance-api-gateway-logs")))
                        .accessLogFormat(AccessLogFormat.jsonWithStandardFields())
                        .build())
                .build());

    new CfnOutput(
        this,
        "lambda-rest-api-url",
        CfnOutputProps.builder().description("RestAPI URL").value(lambdaRestApi.getUrl()).build());
  }
}
