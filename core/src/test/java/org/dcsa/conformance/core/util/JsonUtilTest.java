package org.dcsa.conformance.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  void testIsMissingOrEmpty_withNullNode() {
    assertTrue(JsonUtil.isMissingOrEmpty(null));
  }

  @Test
  void testIsMissingOrEmpty_withMissingNode() {
    JsonNode missingNode = MissingNode.getInstance();
    assertTrue(JsonUtil.isMissingOrEmpty(missingNode));
  }

  @Test
  void testIsMissingOrEmpty_withNullValue() {
    JsonNode nullNode = objectMapper.nullNode();
    assertTrue(JsonUtil.isMissingOrEmpty(nullNode));
  }

  @Test
  void testIsMissingOrEmpty_withEmptyString() throws Exception {
    JsonNode emptyStringNode = objectMapper.readTree(" \"\" ");
    assertTrue(JsonUtil.isMissingOrEmpty(emptyStringNode));
  }

  @Test
  void testIsMissingOrEmpty_withNonEmptyString() throws Exception {
    JsonNode stringNode = objectMapper.readTree("\"hello\"");
    assertFalse(JsonUtil.isMissingOrEmpty(stringNode));
  }

  @Test
  void testIsMissingOrEmpty_withEmptyObject() throws Exception {
    JsonNode emptyObjectNode = objectMapper.readTree("{}");
    assertTrue(JsonUtil.isMissingOrEmpty(emptyObjectNode));
  }

  @Test
  void testIsMissingOrEmpty_withNonEmptyObject() throws Exception {
    JsonNode objectNode = objectMapper.readTree("{\"key\": \"value\"}");
    assertFalse(JsonUtil.isMissingOrEmpty(objectNode));
  }

  @Test
  void testIsMissingOrEmpty_withEmptyArray() throws Exception {
    JsonNode emptyArrayNode = objectMapper.readTree("[]");
    assertTrue(JsonUtil.isMissingOrEmpty(emptyArrayNode));
  }

  @Test
  void testIsMissingOrEmpty_withNonEmptyArray() throws Exception {
    JsonNode arrayNode = objectMapper.readTree("[1, 2, 3]");
    assertFalse(JsonUtil.isMissingOrEmpty(arrayNode));
  }

  @Test
  void testIsMissingOrEmpty_withNumberValue() throws Exception {
    JsonNode numberNode = objectMapper.readTree("42");
    assertFalse(JsonUtil.isMissingOrEmpty(numberNode));
  }

  @Test
  void testIsMissingOrEmpty_withBooleanValue() throws Exception {
    JsonNode booleanNode = objectMapper.readTree("true");
    assertFalse(JsonUtil.isMissingOrEmpty(booleanNode));
  }

  @Test
  void testIsMissing_withNullNode() {
    assertTrue(JsonUtil.isMissing(null));
  }

  @Test
  void testIsMissing_withMissingNode() {
    JsonNode missingNode = MissingNode.getInstance();
    assertTrue(JsonUtil.isMissing(missingNode));
  }

  @Test
  void testIsMissing_withNullValue() {
    JsonNode nullNode = objectMapper.nullNode();
    assertTrue(JsonUtil.isMissing(nullNode));
  }

  @Test
  void testIsMissing_withEmptyString() throws Exception {
    JsonNode emptyStringNode = objectMapper.readTree("\"\"");
    assertFalse(JsonUtil.isMissing(emptyStringNode));
  }

  @Test
  void testIsMissing_withNonEmptyString() throws Exception {
    JsonNode stringNode = objectMapper.readTree("\"hello\"");
    assertFalse(JsonUtil.isMissing(stringNode));
  }

  @Test
  void testIsMissing_withEmptyObject() throws Exception {
    JsonNode emptyObjectNode = objectMapper.readTree("{}");
    assertFalse(JsonUtil.isMissing(emptyObjectNode));
  }

  @Test
  void testIsMissing_withNonEmptyObject() throws Exception {
    JsonNode objectNode = objectMapper.readTree("{\"key\": \"value\"}");
    assertFalse(JsonUtil.isMissing(objectNode));
  }

  @Test
  void testIsMissing_withEmptyArray() throws Exception {
    JsonNode emptyArrayNode = objectMapper.readTree("[]");
    assertFalse(JsonUtil.isMissing(emptyArrayNode));
  }

  @Test
  void testIsMissing_withNonEmptyArray() throws Exception {
    JsonNode arrayNode = objectMapper.readTree("[1, 2, 3]");
    assertFalse(JsonUtil.isMissing(arrayNode));
  }

  @Test
  void testIsMissing_withNumberValue() throws Exception {
    JsonNode numberNode = objectMapper.readTree("42");
    assertFalse(JsonUtil.isMissing(numberNode));
  }

  @Test
  void testIsMissing_withBooleanValue() throws Exception {
    JsonNode booleanNode = objectMapper.readTree("true");
    assertFalse(JsonUtil.isMissing(booleanNode));
  }
}
