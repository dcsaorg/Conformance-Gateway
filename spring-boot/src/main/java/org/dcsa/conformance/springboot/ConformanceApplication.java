package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.state.MemorySortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsLockingMap;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsNonLockingMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Slf4j
@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.springboot")
public class ConformanceApplication {
  @Autowired ConformanceConfiguration conformanceConfiguration;
  private ConformancePersistenceProvider persistenceProvider;

  @PostConstruct
  public void postConstruct() {
    log.info(
        "DcsaConformanceGatewayApplication.postConstruct(%s)"
            .formatted(
                new ObjectMapper()
                    .valueToTree(Objects.requireNonNull(conformanceConfiguration))
                    .toPrettyString()));

    if (conformanceConfiguration.useDynamoDb) {
      log.info("Using DynamoDB persistence provider");
      // docker run -p 127.0.0.1:8000:8000 amazon/dynamodb-local
      // aws dynamodb list-tables --endpoint-url http://localhost:8000
      DynamoDbClient dynamoDbClient =
          DynamoDbClient.builder()
              .endpointOverride(URI.create("http://localhost:8000"))
              .region(Region.EU_NORTH_1)
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create("DummyKey", "DummySecret")))
              .build();
      if (dynamoDbClient.listTables().tableNames().contains("conformance")) {
        dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName("conformance").build());
      }
      dynamoDbClient.createTable(
          CreateTableRequest.builder()
              .tableName("conformance")
              .keySchema(
                  KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
                  KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
              .attributeDefinitions(
                  AttributeDefinition.builder()
                      .attributeName("PK")
                      .attributeType(ScalarAttributeType.S)
                      .build(),
                  AttributeDefinition.builder()
                      .attributeName("SK")
                      .attributeType(ScalarAttributeType.S)
                      .build())
              .billingMode(BillingMode.PAY_PER_REQUEST)
              .provisionedThroughput(
                  ProvisionedThroughput.builder()
                      .readCapacityUnits(0L)
                      .writeCapacityUnits(0L)
                      .build())
              .build());
      persistenceProvider =
          new ConformancePersistenceProvider(
              new DynamoDbSortedPartitionsNonLockingMap(dynamoDbClient, "conformance"),
              new DynamoDbSortedPartitionsLockingMap(dynamoDbClient, "conformance"));
    } else {
      log.info("Using memory map persistence provider");
      persistenceProvider =
          new ConformancePersistenceProvider(
              new MemorySortedPartitionsNonLockingMap(), new MemorySortedPartitionsLockingMap());
    }

    Stream.of(
            "all-in-one",
            "carrier-tested-party",
            "carrier-testing-counterparts",
            "platform-tested-party",
            "platform-testing-counterparts")
        .map(
            baseFileName ->
                ConformanceSandbox.create(
                    persistenceProvider,
                    "eblsurrender-v10-%s".formatted(baseFileName),
                    SandboxConfiguration.fromJsonNode(
                        JsonToolkit.inputStreamToJsonNode(
                            ConformanceApplication.class.getResourceAsStream(
                                "/standards/eblsurrender/v10/%s.json".formatted(baseFileName))))))
        .forEach(sandbox -> log.info("Created sandbox: %s".formatted(sandbox.getId())));
  }

  @GetMapping(value = "/")
  public void handleGetRoot(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    _writeResponse(
        servletResponse, 200, "text/html;charset=utf-8", Collections.emptyMap(), _buildHomePage());
  }

  private String _buildHomePage() {
    return String.join(
        System.lineSeparator(),
        "<html>",
        "<head><title>DCSA Conformance</title></head>",
        "<body style=\"font-family: sans-serif;\">",
        "<h2>DCSA Conformance</h2>",
        _buildHomeSandboxSection("eblsurrender-v10-all-in-one"),
        _buildHomeSandboxSection("eblsurrender-v10-carrier-testing-counterparts"),
        _buildHomeSandboxSection("eblsurrender-v10-platform-testing-counterparts"),
        "</body>",
        "</html>");
  }

  private String _buildHomeSandboxSection(String sandboxId) {
    return String.join(
        System.lineSeparator(),
        "<h3>%s</h3>".formatted(sandboxId),
        "<p><a href=\"/conformance/sandbox/%s/orchestrator/reset\">Reset</a></p>"
            .formatted(sandboxId),
        "<p><a href=\"/conformance/sandbox/%s/orchestrator/status\">Status</a></p>"
            .formatted(sandboxId),
        "<p><a href=\"/conformance/sandbox/%s/orchestrator/party/%s/prompt/json\">Carrier1 prompt</a></p>"
            .formatted(sandboxId, "Carrier1"),
        "<p><a href=\"/conformance/sandbox/%s/orchestrator/party/%s/prompt/json\">Platform1 prompt</a></p>"
            .formatted(sandboxId, "Platform1"),
        "<p><a href=\"/conformance/sandbox/%s/orchestrator/report\">Report</a></p>"
            .formatted(sandboxId));
  }

  @RequestMapping(value = "/conformance/**")
  public void handleRequest(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    ConformanceWebResponse conformanceWebResponse =
        ConformanceSandbox.handleRequest(
            persistenceProvider,
            new ConformanceWebRequest(
                servletRequest.getMethod(),
                servletRequest.getRequestURL().toString(),
                servletRequest.getRequestURI(),
                _getQueryParameters(servletRequest),
                _getRequestHeaders(servletRequest),
                _getRequestBody(servletRequest)));
    _writeResponse(
        servletResponse,
        conformanceWebResponse.statusCode(),
        conformanceWebResponse.contentType(),
        conformanceWebResponse.headers(),
        conformanceWebResponse.body());
  }

  private static Map<String, List<String>> _getQueryParameters(HttpServletRequest request) {
    return request.getParameterMap().entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), Arrays.asList(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<String, List<String>> _getRequestHeaders(HttpServletRequest request) {
    return Collections.list(request.getHeaderNames()).stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                headerName -> Collections.list(request.getHeaders(headerName))));
  }

  @SneakyThrows
  private static void _writeResponse(
      HttpServletResponse servletResponse,
      int statusCode,
      String contentType,
      Map<String, ? extends Collection<String>> headers,
      String stringBody) {
    servletResponse.setStatus(statusCode);
    servletResponse.setContentType(contentType);
    headers.forEach(
        (headerName, headerValues) ->
            headerValues.forEach(
                headerValue -> servletResponse.setHeader(headerName, headerValue)));
    PrintWriter writer = servletResponse.getWriter();
    writer.write(stringBody);
    writer.flush();
  }

  @SneakyThrows
  private static String _getRequestBody(HttpServletRequest request) {
    return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
  }

  public static void main(String[] args) {
    SpringApplication.run(ConformanceApplication.class, args);
  }
}