package org.dcsa.conformance.lambda.auth;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public final class Auth0UserInfoFetcher {

  private final URI endpoint;
  private final HttpClient client;

  public Auth0UserInfoFetcher(String issuer, HttpClient client) {
    this.client = client;
    URI uri = URI.create(issuer);
    if (!"https".equals(uri.getScheme())
      || uri.getHost() == null
      || !"/".equals(uri.getPath())
      || uri.getUserInfo() != null
      || uri.getQuery() != null
      || uri.getFragment() != null
      || uri.getPort() != -1) {
      throw new IllegalArgumentException("Auth0 issuer must be an HTTPS origin with trailing slash");
    }
    endpoint = uri.resolve("userinfo");
  }

  public JsonNode fetch(String token) {
    try {
      var request = HttpRequest.newBuilder(endpoint)
          .timeout(Duration.ofSeconds(8))
          .header("Authorization", "Bearer " + token)
          .header("Accept", "application/json")
          .GET()
          .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200) throw new IllegalStateException();
      byte[] bytes = response.body();
      if (bytes.length > 65536) throw new IllegalStateException();
      JsonNode profile = OBJECT_MAPPER.readTree(bytes);
      if (profile == null || !profile.isObject()) throw new IllegalStateException();
      return profile;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WebuiIdentity.Failure(503, "Identity provider temporarily unavailable");
    } catch (Exception e) {
      // Never propagate remote response bodies or exceptions that may contain credentials/PII.
      throw new WebuiIdentity.Failure(503, "Identity provider temporarily unavailable");
    }
  }
}
