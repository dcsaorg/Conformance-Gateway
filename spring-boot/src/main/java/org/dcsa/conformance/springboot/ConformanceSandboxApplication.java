package org.dcsa.conformance.springboot;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@SpringBootApplication
@ConfigurationPropertiesScan("org.dcsa.conformance.springboot")
public class ConformanceSandboxApplication {
  @Autowired ConformanceConfiguration conformanceConfiguration;
  private ConformanceSandbox conformanceSandbox;

  @PostConstruct
  public void postConstruct() {
    log.info(
        "DcsaConformanceGatewayApplication.postConstruct(%s)"
            .formatted(Objects.requireNonNull(conformanceConfiguration)));

    conformanceSandbox = new ConformanceSandbox(conformanceConfiguration);
  }

  @RequestMapping(value = "/conformance/**")
  public void handleRequest(
      HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    ConformanceWebResponse conformanceWebResponse =
        conformanceSandbox.handleRequest(
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
    SpringApplication.run(ConformanceSandboxApplication.class, args);
  }
}
