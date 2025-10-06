package org.dcsa.conformance.core.party;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.core.UserFacingException;

@Getter
@Setter
@ToString
public class CounterpartConfiguration {
  private boolean inManualMode;
  private String name;
  private String role;
  private String url;
  private String authHeaderName = "";
  private String authHeaderValue = "";
  private HttpHeaderConfiguration[] externalPartyAdditionalHeaders;
  private EndpointUriOverrideConfiguration[] endpointUriOverrideConfigurations;

  public static void validateUrl(String url, boolean allowHttpLocalhost, boolean allowEmptyUrl) throws UserFacingException {
    if (url.isEmpty()) {
      if (allowEmptyUrl) {
        return;
      }
      throw new UserFacingException("The application base URL must not be empty: connecting to your application is not optional for this standard and role.");
    }
    try {
      new URI(url);
    } catch (URISyntaxException e) {
      throw new UserFacingException("The URL format is not correct", e);
    }
    if (!allowHttpLocalhost) {
      String lowerCaseUrl = url.toLowerCase();
      if (!lowerCaseUrl.startsWith("https://")) {
        throw new UserFacingException("The URL must use HTTPS instead of plain HTTP");
      }
      if (Stream.of("https://localhost/", "https://localhost:", "https://127.")
          .anyMatch(lowerCaseUrl::startsWith))
        throw new UserFacingException("The URL cannot be set to a local address");
    }
  }
}
