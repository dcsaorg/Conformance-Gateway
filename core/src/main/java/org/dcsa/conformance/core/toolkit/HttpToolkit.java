package org.dcsa.conformance.core.toolkit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public enum HttpToolkit {
  ; // no instances

  @SneakyThrows
  public static ConformanceResponse syncHttpRequest(ConformanceRequest conformanceRequest) {
    URI uri = conformanceRequest.toURI();
    log.info(
        "HttpToolkit.syncHttpRequest(%s) request: %s"
            .formatted(uri, conformanceRequest.toJson().toPrettyString()));
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder()
            .uri(uri)
            .method(
                conformanceRequest.method(),
                HttpRequest.BodyPublishers.ofString(
                    conformanceRequest.message().body().getStringBody()))
            .timeout(Duration.ofHours(1));
    conformanceRequest
        .message()
        .headers()
        .forEach((name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
    HttpResponse<String> httpResponse =
        HttpClient.newHttpClient()
            .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    ConformanceResponse conformanceResponse =
        conformanceRequest.createResponse(
            httpResponse.statusCode(),
            httpResponse.headers().map(),
            new ConformanceMessageBody(httpResponse.body()));
    log.info(
        "HttpToolkit.syncHttpRequest() response: %s"
            .formatted(conformanceResponse.toJson().toPrettyString()));
    return conformanceResponse;
  }
}
