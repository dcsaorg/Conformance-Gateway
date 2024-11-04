package org.dcsa.conformance.core.toolkit;

import java.net.http.HttpClient;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class IOToolkit {

  public static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();
}
