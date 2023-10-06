package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

@Slf4j
public class HttpClientLambda implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    try {
      JsonNode jsonInput = JsonToolkit.inputStreamToJsonNode(inputStream);
      log.info("jsonInput = " + jsonInput.toPrettyString());
      String url = jsonInput.get("url").asText();
      String authHeaderName = jsonInput.get("authHeaderName").asText();
      String authHeaderValue = jsonInput.get("authHeaderValue").asText();

      HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
      httpRequestBuilder.header(authHeaderName, authHeaderValue);
      int statusCode =
          HttpClient.newHttpClient()
              .send(
                  httpRequestBuilder.build(),
                  HttpResponse.BodyHandlers.ofString())
              .statusCode();

      ObjectNode jsonOutput = new ObjectMapper().createObjectNode().put("statusCode", statusCode);
      log.info("jsonOutput = " + jsonOutput.toPrettyString());
      try (BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
        writer.write(jsonOutput.toPrettyString());
        writer.flush();
      }
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: " + e, e);
      throw e;
    } catch (IOException | InterruptedException e) {
      log.error("Unhandled exception: " + e, e);
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {} // unused
}
