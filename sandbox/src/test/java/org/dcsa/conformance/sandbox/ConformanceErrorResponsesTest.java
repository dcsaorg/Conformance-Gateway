package org.dcsa.conformance.sandbox;

import org.dcsa.conformance.core.UserFacingException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConformanceErrorResponsesTest {

  @Test
  void shouldPreserveUserFacingWebuiErrors() {
    var response = ConformanceErrorResponses.webuiResponse(
      LoggerFactory.getLogger(getClass()),
      "testing a user-facing error",
      new UserFacingException("Safe validation message")
    );

    assertEquals("Safe validation message", response.path("error").asText());
    assertFalse(response.has("errorId"));
  }

  @Test
  void shouldMaskUnexpectedWebuiErrors() {
    var response = ConformanceErrorResponses.webuiResponse(
      LoggerFactory.getLogger(getClass()),
      "testing an unexpected Web UI error",
      new IllegalStateException("ClassNotFoundException: leaked detail")
    );

    assertEquals(ConformanceErrorResponses.UNEXPECTED_ERROR_MESSAGE, response.path("error").asText());
    assertTrue(response.hasNonNull("errorId"));
    assertFalse(response.toPrettyString().contains("ClassNotFoundException"));
  }

  @Test
  void shouldMaskUnexpectedApiErrors() throws Exception {
    var response = ConformanceErrorResponses.unexpectedApiResponse(
      LoggerFactory.getLogger(getClass()),
      "testing an unexpected API error",
      new IllegalStateException("ClassNotFoundException: leaked detail")
    );

    var responseBody = OBJECT_MAPPER.readTree(response.body());

    assertEquals(500, response.statusCode());
    assertEquals(ConformanceErrorResponses.UNEXPECTED_ERROR_MESSAGE, responseBody.path("error").asText());
    assertTrue(responseBody.hasNonNull("errorId"));
    assertFalse(response.body().contains("ClassNotFoundException"));
  }
}



