package org.dcsa.conformance.core.toolkit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Stream;

import org.dcsa.conformance.core.UserFacingException;

/**
 * A validated URL value object. Once created, the URL is guaranteed to be valid
 * according to the validation rules applied during construction.
 *
 * This class supports Jackson serialization - it serializes to/from a plain string.
 */
public final class Url {

  /** An empty URL instance, useful as a default value. */
  public static final Url EMPTY = new Url("");

  private final String value;

  private Url(String value) {
    this.value = value;
  }

  /**
   * Creates a new validated URL.
   *
   * @param url               the URL string to validate
   * @param allowHttpLocalhost whether to allow HTTP and localhost URLs
   * @param allowEmpty        whether to allow empty URLs
   * @return a validated Url instance
   * @throws UserFacingException if the URL is invalid
   */
  public static Url of(String url, boolean allowHttpLocalhost, boolean allowEmpty) throws UserFacingException {
    validate(url, allowHttpLocalhost, allowEmpty);
    return new Url(url != null ? url : "");
  }

  /**
   * Creates a URL without validation. Use only when the URL comes from a trusted source
   * or when deserializing from JSON (validation should happen at the application boundary).
   *
   * @param url the URL string
   * @return a Url instance
   */
  @JsonCreator
  public static Url ofTrusted(String url) {
    return new Url(url != null ? url : "");
  }

  /**
   * Validates a URL string without creating a Url object.
   *
   * @param url               the URL string to validate
   * @param allowHttpLocalhost whether to allow HTTP and localhost URLs
   * @param allowEmpty        whether to allow empty URLs
   * @throws UserFacingException if the URL is invalid
   */
  public static void validate(String url, boolean allowHttpLocalhost, boolean allowEmpty) throws UserFacingException {
    if (url == null || url.isEmpty()) {
      if (allowEmpty) {
        return;
      }
      throw new UserFacingException("The application base URL must not be empty: connecting to your application is not optional for this standard and role.");
    }
    try {
      new URI(url);
    } catch (URISyntaxException e) {
      throw new UserFacingException("The URL format is not correct", e);
    }
    if (url.contains("?")) {
      throw new UserFacingException("The URL must not contain query parameters (question marks)");
    }
    if (!allowHttpLocalhost) {
      String lowerCaseUrl = url.toLowerCase();
      if (!lowerCaseUrl.startsWith("https://")) {
        throw new UserFacingException("The URL must use HTTPS instead of plain HTTP");
      }
      if (Stream.of("https://localhost/", "https://localhost:", "https://127.")
          .anyMatch(lowerCaseUrl::startsWith)) {
        throw new UserFacingException("The URL cannot be set to a local address");
      }
    }
  }

  /**
   * Returns the URL string value.
   */
  @JsonValue
  public String getValue() {
    return value;
  }

  /**
   * Returns true if this URL is empty or blank.
   */
  public boolean isBlank() {
    return value.isBlank();
  }

  /**
   * Returns the length of the URL string.
   */
  public int length() {
    return value.length();
  }


  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Url url = (Url) o;
    return Objects.equals(value, url.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}


