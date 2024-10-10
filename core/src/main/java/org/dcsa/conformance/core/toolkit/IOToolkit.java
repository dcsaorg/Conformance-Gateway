package org.dcsa.conformance.core.toolkit;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class IOToolkit {

  public static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(30))
          .build();
}
