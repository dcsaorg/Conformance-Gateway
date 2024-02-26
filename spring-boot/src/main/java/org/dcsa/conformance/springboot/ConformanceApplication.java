package org.dcsa.conformance.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.state.MemorySortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsLockingMap;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsNonLockingMap;
import org.dcsa.conformance.standards.booking.BookingComponentFactory;
import org.dcsa.conformance.standards.ebl.EblComponentFactory;
import org.dcsa.conformance.standards.eblissuance.EblIssuanceComponentFactory;
import org.dcsa.conformance.standards.eblsurrender.EblSurrenderComponentFactory;
import org.dcsa.conformance.standards.ovs.OvsComponentFactory;
import org.dcsa.conformance.standards.tnt.TntComponentFactory;
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
  private final ConformanceConfiguration conformanceConfiguration;
  private final ConformancePersistenceProvider persistenceProvider;

  ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  @Getter
  private final Consumer<JsonNode> deferredSandboxTaskConsumer;

  private final ConformanceAccessChecker accessChecker =
      new ConformanceAccessChecker() {
        @Override
        public String getUserEnvironmentId(String userId) {
          return "spring-boot-env";
        }

        @Override
        public void checkUserSandboxAccess(String userId, String sandboxId) {
          // full access
        }
      };

  private final String localhostAuthUrlToken = UUID.randomUUID().toString();

  private final LinkedList<String> homepageSandboxIds = new LinkedList<>();

  public ConformanceApplication(ConformanceConfiguration conformanceConfiguration) {
    this.conformanceConfiguration = conformanceConfiguration;
    log.info(
        "new ConformanceApplication(%s)"
            .formatted(
                new ObjectMapper()
                    .valueToTree(Objects.requireNonNull(this.conformanceConfiguration))
                    .toPrettyString()));

    DynamoDbClient dynamoDbClient;
    if (this.conformanceConfiguration.useDynamoDb) {
      log.info("Using DynamoDB persistence provider");
      // docker run -p 127.0.0.1:8000:8000 amazon/dynamodb-local
      // aws dynamodb list-tables --endpoint-url http://localhost:8000
      dynamoDbClient =
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
    deferredSandboxTaskConsumer =
        jsonNode ->
            executor.schedule(
                () -> {
                  try {
                    ConformanceSandbox.executeDeferredTask(
                        persistenceProvider, getDeferredSandboxTaskConsumer(), jsonNode);
                  } catch (Exception e) {
                    log.error("Deferred sandbox task execution failed", e);
                  }
                },
                100,
                TimeUnit.MILLISECONDS);

    Stream<AbstractComponentFactory> componentFactories =
        Stream.of(
                BookingComponentFactory.STANDARD_VERSIONS.stream()
                    .map(BookingComponentFactory::new),
                EblComponentFactory.STANDARD_VERSIONS.stream().map(EblComponentFactory::new),
                EblIssuanceComponentFactory.STANDARD_VERSIONS.stream()
                    .map(EblIssuanceComponentFactory::new),
                EblSurrenderComponentFactory.STANDARD_VERSIONS.stream()
                    .map(EblSurrenderComponentFactory::new),
                OvsComponentFactory.STANDARD_VERSIONS.stream().map(OvsComponentFactory::new),
                TntComponentFactory.STANDARD_VERSIONS.stream().map(TntComponentFactory::new))
            .flatMap(Function.identity());
    componentFactories.forEach(
        componentFactory -> {
          ArrayList<String> roleNames = new ArrayList<>(componentFactory.getRoleNames());
          String roleOne = roleNames.get(0);
          String roleTwo = roleNames.get(1);
          Stream.concat(
                  this.conformanceConfiguration.createAutoTestingSandboxes
                      ? Stream.of(
                          componentFactory.getJsonSandboxConfigurationTemplate(null, false, false),
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleOne, false, false),
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleOne, false, true),
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleTwo, false, false),
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleTwo, false, true))
                      : Stream.of(),
                  this.conformanceConfiguration.createManualTestingSandboxes
                      ? Stream.of(
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleOne, true, false),
                          componentFactory.getJsonSandboxConfigurationTemplate(roleOne, true, true),
                          componentFactory.getJsonSandboxConfigurationTemplate(
                              roleTwo, true, false),
                          componentFactory.getJsonSandboxConfigurationTemplate(roleTwo, true, true))
                      : Stream.of())
              .forEach(
                  jsonSandboxConfigurationTemplate -> {
                    String sandboxId = jsonSandboxConfigurationTemplate.get("id").asText();
                    if (sandboxId.contains("-auto-")
                        && (sandboxId.contains("all-in-one")
                            || sandboxId.contains("testing-counterparts"))) {
                      homepageSandboxIds.add(sandboxId);
                    }
                    ConformanceSandbox.create(
                        persistenceProvider,
                        deferredSandboxTaskConsumer,
                        "spring-boot-env",
                        SandboxConfiguration.fromJsonNode(jsonSandboxConfigurationTemplate));
                  });
        });
  }

  @CrossOrigin(origins = "http://localhost:4200")
  @RequestMapping(value = "/conformance/webui/**")
  public void handleWebuiRequest(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    _writeResponse(
        servletResponse,
        200,
        "application/json;charset=utf-8",
        Collections.emptyMap(),
        new ConformanceWebuiHandler(
                accessChecker,
                "http://localhost:8080",
                persistenceProvider,
                deferredSandboxTaskConsumer)
            .handleRequest(
                "spring-boot-env", JsonToolkit.stringToJsonNode(_getRequestBody(servletRequest)))
            .toPrettyString());
  }

  @RequestMapping(value = "/conformance/**")
  public void handleRequest(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {

    String requestUrl = servletRequest.getRequestURL().toString();
    Map<String, List<String>> requestHeaders = _getRequestHeaders(servletRequest);
    String uriAuthPrefix = "/conformance/" + localhostAuthUrlToken + "/sandbox/";
    if (requestUrl.contains(uriAuthPrefix)) {
      int sandboxIdStart = requestUrl.indexOf(uriAuthPrefix) + uriAuthPrefix.length();
      int sandboxIdEnd = requestUrl.indexOf("/", sandboxIdStart);
      String sandboxId = requestUrl.substring(sandboxIdStart, sandboxIdEnd);

      requestUrl = requestUrl.replaceAll(localhostAuthUrlToken + "/", "");

      requestHeaders = new HashMap<>(requestHeaders);
      SandboxConfiguration sandboxConfiguration =
          ConformanceSandbox.loadSandboxConfiguration(persistenceProvider, sandboxId);
      if (!sandboxConfiguration.getAuthHeaderName().isBlank()) {
        requestHeaders.put(
            sandboxConfiguration.getAuthHeaderName(),
            List.of(sandboxConfiguration.getAuthHeaderValue()));
      }
    }

    ConformanceWebResponse conformanceWebResponse =
        ConformanceSandbox.handleRequest(
            persistenceProvider,
            new ConformanceWebRequest(
                servletRequest.getMethod(),
                requestUrl,
                _getQueryParameters(servletRequest),
                requestHeaders,
                _getRequestBody(servletRequest)),
            deferredSandboxTaskConsumer);

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

  @GetMapping(value = "/")
  public void handleGetRoot(
      HttpServletRequest ignoredServletRequest, HttpServletResponse servletResponse) {
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
        homepageSandboxIds.stream()
            .map(this::_buildHomeSandboxSection)
            .collect(Collectors.joining(System.lineSeparator())),
        "</body>",
        "</html>");
  }

  private String _buildHomeSandboxSection(String sandboxId) {
    return String.join(
        System.lineSeparator(),
        "<h3>%s</h3>".formatted(sandboxId),
        "<p>",
        "<a href=\"/conformance/%s/sandbox/%s/reset\">Reset</a> - "
            .formatted(localhostAuthUrlToken, sandboxId),
        "<a href=\"/conformance/%s/sandbox/%s/status\">Status</a> - "
            .formatted(localhostAuthUrlToken, sandboxId),
        "<a href=\"/conformance/%s/sandbox/%s/report\">Report</a> - "
            .formatted(localhostAuthUrlToken, sandboxId),
        "<a href=\"/conformance/%s/sandbox/%s/printableReport\">Printable</a> - "
            .formatted(localhostAuthUrlToken, sandboxId),
        "<a href=\"/conformance/%s/sandbox/%s/party/%s/prompt/json\">Carrier1 prompt</a> - "
            .formatted(localhostAuthUrlToken, sandboxId, "Carrier1"),
        "<a href=\"/conformance/%s/sandbox/%s/party/%s/prompt/json\">Platform1 prompt</a>"
            .formatted(localhostAuthUrlToken, sandboxId, "Platform1"),
        "</p>");
  }

  public static void main(String[] args) {
    // System.setProperty("javax.net.debug", "ssl:all");
    SpringApplication.run(ConformanceApplication.class, args);
  }
}
