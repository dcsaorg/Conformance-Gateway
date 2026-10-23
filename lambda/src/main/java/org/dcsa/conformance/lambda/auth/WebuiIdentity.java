package org.dcsa.conformance.lambda.auth;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Only API Gateway authorizer context is trusted; request body identity fields are never used.
 */
@FunctionalInterface
public interface WebuiIdentity {

  String resolve(JsonNode claims, String authorization);

  class Failure extends RuntimeException {
    private final int status;

    public Failure(int status, String message) {
      super(message);
      this.status = status;
    }

    public int status() {
      return status;
    }
  }
}
