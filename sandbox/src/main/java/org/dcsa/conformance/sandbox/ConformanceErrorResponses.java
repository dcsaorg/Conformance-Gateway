package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.UUID;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public final class ConformanceErrorResponses {

  public static final String UNEXPECTED_ERROR_MESSAGE =
    "An unexpected error occurred while processing the request. Please try again later or contact support if the problem persists.";

  private ConformanceErrorResponses() {
  }

  public static JsonNode webuiResponse(Logger log, String context, RuntimeException exception) {
    if (exception instanceof UserFacingException userFacingException) {
      return OBJECT_MAPPER.createObjectNode().put("error", userFacingException.getMessage());
    }
    return unexpectedWebuiResponse(log, context, exception);
  }

  public static JsonNode unexpectedWebuiResponse(Logger log, String context, RuntimeException exception) {
    String errorId = _logUnexpectedError(log, context, exception);
    return OBJECT_MAPPER.createObjectNode()
      .put("error", UNEXPECTED_ERROR_MESSAGE)
      .put("errorId", errorId);
  }

  public static ConformanceWebResponse unexpectedApiResponse(
    Logger log, String context, RuntimeException exception) {
    String errorId = _logUnexpectedError(log, context, exception);
    return new ConformanceWebResponse(
      500,
      JsonToolkit.JSON_UTF_8,
      Collections.emptyMap(),
      OBJECT_MAPPER
        .createObjectNode()
        .put("error", UNEXPECTED_ERROR_MESSAGE)
        .put("errorId", errorId)
        .toPrettyString());
  }

  private static String _logUnexpectedError(Logger log, String context, RuntimeException exception) {
    String errorId = UUID.randomUUID().toString();
    log.error("Unexpected error [{}] while {}", errorId, context, exception);
    return errorId;
  }
}
