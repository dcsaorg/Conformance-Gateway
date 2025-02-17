package org.dcsa.conformance.cdk;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.awscdk.BundlingOptions;
import software.amazon.awscdk.DockerVolume;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.aws_apigatewayv2_authorizers.HttpUserPoolAuthorizer;
import software.amazon.awscdk.aws_apigatewayv2_authorizers.HttpUserPoolAuthorizerProps;
import software.amazon.awscdk.aws_apigatewayv2_integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.DomainNameOptions;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.LambdaRestApiProps;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.CfnStage;
import software.amazon.awscdk.services.apigatewayv2.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.DomainMappingOptions;
import software.amazon.awscdk.services.apigatewayv2.DomainName;
import software.amazon.awscdk.services.apigatewayv2.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.IDomainName;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.DistributionProps;
import software.amazon.awscdk.services.cloudfront.ErrorResponse;
import software.amazon.awscdk.services.cloudfront.IOrigin;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.cognito.AccountRecovery;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.SignInAliases;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientOptions;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.IPublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZoneAttributes;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGateway;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.BucketDeploymentProps;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

public class ConformanceStack extends Stack {
  public ConformanceStack(
      Construct parent,
      StackProps stackProps,
      String stackName,
      String prefix,
      String hostedZoneId,
      String hostedZoneName,
      String lambdaInsightsArn,
      String restApiCertificateArn,
      String webuiApiCertificateArn,
      String webuiDistCertificateArn) {
    super(parent, stackName, stackProps);

    UserPool userPool =
        UserPool.Builder.create(this, prefix + "UserPool")
            .userPoolName(prefix + "UserPool")
            .selfSignUpEnabled(false)
            .signInAliases(SignInAliases.builder().email(true).build())
            .accountRecovery(AccountRecovery.EMAIL_ONLY)
            .passwordPolicy(
                PasswordPolicy.builder()
                    .minLength(8)
                    .requireLowercase(true)
                    .requireUppercase(true)
                    .requireDigits(true)
                    .requireSymbols(false)
                    .build())
            .build();

    UserPoolClient userPoolClient =
        userPool.addClient(
            prefix + "UserPoolClient",
            UserPoolClientOptions.builder()
                .authFlows(AuthFlow.builder().userSrp(true).build())
                .preventUserExistenceErrors(true)
                .build());

    HttpUserPoolAuthorizer httpUserPoolAuthorizer =
        new HttpUserPoolAuthorizer(
            prefix + "HttpUserPoolAuthorizer",
            userPool,
            HttpUserPoolAuthorizerProps.builder().userPoolClients(List.of(userPoolClient)).build());

    String tableName = prefix + "Conformance";
    Table table =
        new Table(
            this,
            prefix + "ConformanceTable",
            TableProps.builder()
                .tableName(tableName)
                .partitionKey(Attribute.builder().name("PK").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("SK").type(AttributeType.STRING).build())
                .pointInTimeRecovery(true)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

    Vpc vpc = Vpc.Builder.create(this, prefix + "ConformanceVpc").maxAzs(1).natGateways(1).build();

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
                                "cp /asset-input/target/conformance-lambda.jar /asset-output/"))
                        .image(Runtime.JAVA_21.getBundlingImage())
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

    Function sandboxTaskLambda =
        new Function(
            this,
            prefix + "SandboxTaskLambda",
            FunctionProps.builder()
                .functionName(prefix + "SandboxTaskLambda")
                .runtime(Runtime.JAVA_21)
                .code(assetCode)
                .handler("org.dcsa.conformance.lambda.SandboxTaskLambda")
                .vpc(vpc)
                .vpcSubnets(
                    SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
                .memorySize(1024)
                .timeout(Duration.minutes(5))
                .reservedConcurrentExecutions(16)
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());

    Function apiLambda =
        new Function(
            this,
            prefix + "ApiLambda",
            FunctionProps.builder()
                .functionName(prefix + "ApiLambda")
                .runtime(Runtime.JAVA_21)
                .code(assetCode)
                .handler("org.dcsa.conformance.lambda.ApiLambda")
                .vpc(vpc)
                .vpcSubnets(
                    SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
                .memorySize(1024)
                .timeout(Duration.minutes(5))
                .reservedConcurrentExecutions(16)
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());

    Function webuiLambda =
        new Function(
            this,
            prefix + "WebuiLambda",
            FunctionProps.builder()
                .functionName(prefix + "WebuiLambda")
                .runtime(Runtime.JAVA_21)
                .code(assetCode)
                .handler("org.dcsa.conformance.lambda.WebuiLambda")
                .vpc(vpc)
                .vpcSubnets(
                    SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
                .memorySize(1024)
                .timeout(Duration.minutes(5))
                .reservedConcurrentExecutions(16)
                .logRetention(RetentionDays.SEVEN_YEARS)
                .build());

    Policy invokeSandboxTaskLambdaPolicy =
        new Policy(
            this,
            prefix + "InvokeSandboxTaskLambdaPolicy",
            PolicyProps.builder()
                .statements(
                    List.of(
                        PolicyStatement.Builder.create()
                            .actions(List.of("lambda:InvokeFunction"))
                            .resources(List.of(sandboxTaskLambda.getFunctionArn()))
                            .build()))
                .build());

    Stream.of(sandboxTaskLambda, apiLambda, webuiLambda)
        .forEach(
            lambda -> {
              table.grantReadWriteData(lambda);
              lambda.addEnvironment("TABLE_NAME", tableName);

              Objects.requireNonNull(lambda.getRole())
                  .attachInlinePolicy(invokeSandboxTaskLambdaPolicy);

              Objects.requireNonNull(lambda.getRole())
                  .addManagedPolicy(
                      ManagedPolicy.fromAwsManagedPolicyName(
                          "CloudWatchLambdaInsightsExecutionRolePolicy"));
              lambda.addLayers(
                  LayerVersion.fromLayerVersionArn(
                      this, lambda.getFunctionName() + "-insights-layer", lambdaInsightsArn));
            });

    String lambdaRestApiUrl = "%s-api.%s".formatted(prefix, hostedZoneName);
    LambdaRestApi lambdaRestApi =
        new LambdaRestApi(
            this,
            prefix + "LambdaRestApi",
            LambdaRestApiProps.builder()
                .restApiName(prefix + "LambdaRestApi")
                .handler(apiLambda)
                .proxy(true)
                .domainName(
                    DomainNameOptions.builder()
                        .domainName(lambdaRestApiUrl)
                        .certificate(
                            Certificate.fromCertificateArn(
                                this, prefix + "LambdaRestApiCertificate", restApiCertificateArn))
                        .build())
                .deployOptions(
                    StageOptions.builder()
                        .accessLogDestination(
                            new LogGroupLogDestination(
                                new LogGroup(this, prefix + "LambdaRestApiGatewayLogs")))
                        .accessLogFormat(AccessLogFormat.jsonWithStandardFields())
                        .build())
                .build());

    String webuiApiGatewayUrl = "%s-webui.%s".formatted(prefix, hostedZoneName);
    String webuiDistributionUrl = "%s.%s".formatted(prefix, hostedZoneName);
    IDomainName webuiApiDomainName =
        new DomainName(
            this,
            prefix + "WebuiApiDomainName",
            DomainNameProps.builder()
                .domainName(webuiApiGatewayUrl)
                .certificate(
                    Certificate.fromCertificateArn(
                        this, prefix + "WebuiApiCertificate", webuiApiCertificateArn))
                .build());
    HttpApi httpApi =
        new HttpApi(
            this,
            prefix + "WebuiHttpApi",
            HttpApiProps.builder()
                .apiName(prefix + "WebuiHttpApi")
                .corsPreflight(
                    CorsPreflightOptions.builder()
                        .allowHeaders(
                            List.of("Authorization", "Content-Type", "dcsa-conformance-api-key"))
                        .allowMethods(
                            List.of(
                                CorsHttpMethod.GET,
                                CorsHttpMethod.HEAD,
                                CorsHttpMethod.OPTIONS,
                                CorsHttpMethod.POST))
                        .allowOrigins(List.of("https://" + webuiDistributionUrl))
                        .maxAge(Duration.hours(1))
                        .build())
                .createDefaultStage(true)
                .defaultDomainMapping(
                    DomainMappingOptions.builder().domainName(webuiApiDomainName).build())
                .build());
    httpApi.addRoutes(
        AddRoutesOptions.builder()
            .authorizer(httpUserPoolAuthorizer)
            .path("/conformance/webui")
            .methods(List.of(HttpMethod.GET, HttpMethod.POST))
            .integration(new HttpLambdaIntegration(prefix + "WebuiLambdaIntegration", webuiLambda))
            .build());

    LogGroup httpApiAccessLogGroup = new LogGroup(this, prefix + "HttpApiAccessLogGroup");
    ((CfnStage)
            Objects.requireNonNull(
                Objects.requireNonNull(httpApi.getDefaultStage()).getNode().getDefaultChild()))
        .setAccessLogSettings(
            CfnStage.AccessLogSettingsProperty.builder()
                .destinationArn(httpApiAccessLogGroup.getLogGroupArn())
                // https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-mapping-template-reference.html#context-variable-reference-access-logging-only
                .format(
                    new ObjectMapper()
                        .createObjectNode()
                        .put("userArn", "$context.identity.userArn")
                        .put("principalId", "$context.authorizer.principalId")
                        .put("sourceIp", "$context.identity.sourceIp")
                        .put("userAgent", "$context.identity.userAgent")
                        .put("requestId", "$context.requestId")
                        .put("requestTime", "$context.requestTime")
                        .put("protocol", "$context.protocol")
                        .put("httpMethod", "$context.httpMethod")
                        .put("path", "$context.path")
                        .put("status", "$context.status")
                        .put("errorMessageString", "$context.error.messageString")
                        .put("responseLength", "$context.responseLength")
                        .toString())
                .build());
    httpApiAccessLogGroup.grantWrite(new ServicePrincipal("apigateway.amazonaws.com"));

    Bucket ngBucket = new Bucket(this, prefix + "NgBucket", BucketProps.builder().build());
    IOrigin s3Origin = S3BucketOrigin.withOriginAccessControl(ngBucket);
    Distribution distribution =
        new Distribution(
            this,
            prefix + "Distribution",
            DistributionProps.builder()
                .defaultBehavior(
                    BehaviorOptions.builder()
                        .cachePolicy(CachePolicy.CACHING_DISABLED)
                        .origin(s3Origin)
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .build())
                .domainNames(List.of(webuiDistributionUrl))
                .certificate(
                    Certificate.fromCertificateArn(
                        this, prefix + "WebuiCertificate", webuiDistCertificateArn))
                .defaultRootObject("index.html")
                .errorResponses(
                    List.of(
                        ErrorResponse.builder()
                            .httpStatus(403)
                            .responseHttpStatus(200)
                            .responsePagePath("/index.html")
                            .ttl(Duration.seconds(0))
                            .build(),
                        ErrorResponse.builder()
                            .httpStatus(404)
                            .responseHttpStatus(200)
                            .responsePagePath("/index.html")
                            .ttl(Duration.seconds(0))
                            .build()))
                .build());

    new BucketDeployment(
        this,
        prefix + "BucketDeployment",
        BucketDeploymentProps.builder()
            .destinationBucket(ngBucket)
            .distribution(distribution)
            .sources(List.of(Source.asset("../webui/dist/webui/browser")))
            .build());

    IPublicHostedZone publicHostedZone =
        PublicHostedZone.fromPublicHostedZoneAttributes(
            this,
            prefix + "PublicHostedZone",
            PublicHostedZoneAttributes.builder()
                .hostedZoneId(hostedZoneId)
                .zoneName(hostedZoneName)
                .build());
    new ARecord(
        this,
        prefix + "RestApiARecord",
        ARecordProps.builder()
            .zone(publicHostedZone)
            .recordName(lambdaRestApiUrl)
            .target(RecordTarget.fromAlias(new ApiGateway(lambdaRestApi)))
            .build());
    new ARecord(
        this,
        prefix + "WebuiApiARecord",
        ARecordProps.builder()
            .zone(publicHostedZone)
            .recordName(webuiApiGatewayUrl)
            .target(
                RecordTarget.fromAlias(
                    new ApiGatewayv2DomainProperties(
                        webuiApiDomainName.getRegionalDomainName(),
                        webuiApiDomainName.getRegionalHostedZoneId())))
            .build());
    new ARecord(
        this,
        prefix + "WebuiDistARecord",
        ARecordProps.builder()
            .zone(publicHostedZone)
            .recordName(webuiDistributionUrl)
            .target(RecordTarget.fromAlias(new CloudFrontTarget(distribution)))
            .build());
  }
}
