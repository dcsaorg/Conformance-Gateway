package org.dcsa.conformance.core;

/**
 * Exception to throw when the message is safe to display and meaningful for the manual operator.
 */
public class UserFacingException extends RuntimeException {
  public UserFacingException(String message) {
    super(message);
  }

  public UserFacingException(String message, Throwable cause) {
    super(message, cause);
  }
}
