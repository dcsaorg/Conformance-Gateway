package org.dcsa.conformance.core.toolkit;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class IOToolkit {

  public static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

  @SneakyThrows
  public static String templateFileToText(String templatePath, Map<String, String> replacements) {
    AtomicReference<String> fileContent = new AtomicReference<>();
    try (InputStream inputStream = JsonToolkit.class.getResourceAsStream(templatePath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Could not find file: " + templatePath);
      }
      fileContent.set(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
    }
    if (replacements != null)
      replacements.forEach((key, value) -> fileContent.set(fileContent.get().replace(key, value)));
    return fileContent.get();
  }
}
