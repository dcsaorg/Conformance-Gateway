package org.dcsa.conformance.lambda.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class Auth0UserInfoFetcherTest {
  @Test
  void onlyConfiguredHttpsIssuerIsAllowed() {
    for (String issuer :
        new String[] {
          "http://tenant.example/",
          "https://tenant.example/path/",
          "https://user@tenant.example/",
          "https://tenant.example/?endpoint=evil",
          "https://tenant.example:8443/"
        }) {
      assertThrows(
          IllegalArgumentException.class, () -> new Auth0UserInfoFetcher(issuer, mock(HttpClient.class)));
    }
  }

  @Test
  void productionClientHasBoundedConnectionsAndNeverFollowsRedirects() {
    try (var client = WebuiAuthentication.userInfoHttpClient()) {
      assertEquals(java.time.Duration.ofSeconds(5), client.connectTimeout().orElseThrow());
      assertEquals(HttpClient.Redirect.NEVER, client.followRedirects());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void requestsUserinfoWithBoundedTimeoutAndDoesNotExposeRemoteErrors() throws Exception {
    HttpClient client = mock(HttpClient.class);
    HttpResponse<byte[]> response = mock(HttpResponse.class);
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn("{\"sub\":\"user\",\"email_verified\":true}".getBytes(StandardCharsets.UTF_8));
    var info = new Auth0UserInfoFetcher("https://tenant.example/", client);
    assertEquals("user", info.fetch("access-token").path("sub").asText());
    var request = ArgumentCaptor.forClass(HttpRequest.class);
    verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
    assertEquals("https://tenant.example/userinfo", request.getValue().uri().toString());
    assertEquals(
        "Bearer access-token",
        request.getValue().headers().firstValue("Authorization").orElseThrow());
    assertTrue(request.getValue().timeout().orElseThrow().toSeconds() <= 10);
    for (int status : new int[] {301, 401, 403, 429, 500}) {
      when(response.statusCode()).thenReturn(status);
      var failure = assertThrows(WebuiIdentity.Failure.class, () -> info.fetch("private-token"));
      assertEquals(503, failure.status());
      assertFalse(failure.getMessage().contains("private-token"));
    }
    when(response.statusCode()).thenReturn(200);
    for (byte[] body :
        new byte[][] {
          "invalid".getBytes(StandardCharsets.UTF_8),
          "null".getBytes(StandardCharsets.UTF_8),
          new byte[65537]
        }) {
      when(response.body()).thenReturn(body);
      assertThrows(WebuiIdentity.Failure.class, () -> info.fetch("token"));
    }
    when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new java.net.http.HttpTimeoutException("remote failure"));
    assertEquals(
        503, assertThrows(WebuiIdentity.Failure.class, () -> info.fetch("token")).status());
  }
}
