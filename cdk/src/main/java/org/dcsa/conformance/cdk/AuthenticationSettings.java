package org.dcsa.conformance.cdk;

import java.net.URI;
import java.util.Map;

/**
 * Validated public Auth0 configuration shared by one stack's backend and frontend.
 */
public record AuthenticationSettings(String issuer, String audience, String clientId) {

  public AuthenticationSettings {
    requireValue("issuer", issuer);
    requireValue("audience", audience);
    requireValue("clientId", clientId);
    URI uri = URI.create(issuer);
    if (!"https".equals(uri.getScheme())
      || uri.getHost() == null
      || !"/".equals(uri.getPath())
      || uri.getUserInfo() != null
      || uri.getQuery() != null
      || uri.getFragment() != null
      || uri.getPort() != -1
      || audience.equals(clientId)
      || audience.equals(issuer + "userinfo")) {
      throw new IllegalArgumentException("Invalid Auth0 issuer or API audience");
    }
  }

  public static AuthenticationSettings fromContext(Object context) {
    if (!(context instanceof Map<?, ?> config)) {
      throw new IllegalArgumentException("Invalid Auth0 context");
    }
    if (config.containsKey("provider")) {
      throw new IllegalArgumentException(
        "Provider selection has been removed; configure Auth0 issuer, audience and clientId");
    }
    return new AuthenticationSettings(
      value(config, "issuer"), value(config, "audience"), value(config, "clientId"));
  }

  private static String value(Map<?, ?> config, String key) {
    Object value = config.get(key);
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException("Missing or invalid Auth0 setting: " + key);
    }
    return text;
  }

  private static void requireValue(String key, String text) {
    if (text == null
      || text.isBlank()
      || !text.equals(text.trim())
      || text.contains("<")
      || text.contains(">")
      || text.contains(" ")) {
      throw new IllegalArgumentException("Missing or invalid Auth0 setting: " + key);
    }
  }
}
