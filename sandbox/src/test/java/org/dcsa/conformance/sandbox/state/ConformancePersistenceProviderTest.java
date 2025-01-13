package org.dcsa.conformance.sandbox.state;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.state.MemorySortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.MemorySortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.state.StatefulExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConformancePersistenceProviderTest {
  private static final int TEST_MAX_VALUE_LENGTH = 128;
  private SortedPartitionsNonLockingMap nonLockingMap;
  private StatefulExecutor statefulExecutor;

  @BeforeEach
  void setUp() {
    ConformancePersistenceProvider conformancePersistenceProvider =
        new ConformancePersistenceProvider(
            new MemorySortedPartitionsNonLockingMap(),
            new MemorySortedPartitionsLockingMap(),
            TEST_MAX_VALUE_LENGTH);
    nonLockingMap = conformancePersistenceProvider.getNonLockingMap();
    statefulExecutor = conformancePersistenceProvider.getStatefulExecutor();
  }

  @Test
  void nonLockingMapSmallValuesWorkNormally() {
    Stream.of("testValue", 123, true)
        .forEach(
            rawValue -> {
              JsonNode writtenNode = OBJECT_MAPPER.valueToTree(rawValue);
              nonLockingMap.setItemValue("testPK", "testSK", writtenNode);
              JsonNode readNode = nonLockingMap.getItemValue("testPK", "testSK");
              assertEquals(writtenNode, readNode);
            });
  }

  @Test
  void nonLockingMapSmallJsonWorksNormally() {
    JsonNode writtenNode =
        OBJECT_MAPPER.createObjectNode().put("keyOne", "valueOne").put("keyTwo", "valueTwo");
    nonLockingMap.setItemValue("testPK", "testSK", writtenNode);
    JsonNode readNode = nonLockingMap.getItemValue("testPK", "testSK");
    assertEquals(writtenNode, readNode);
  }

  @Test
  void nonLockingMapLargeJsonWorksNormally() {
    JsonNode writtenNode = createNestedJsonNode(1);
    nonLockingMap.setItemValue("testPK", "testSK", writtenNode);
    JsonNode readNode = nonLockingMap.getItemValue("testPK", "testSK");
    assertEquals(writtenNode, readNode);
  }

  @Test
  void executorStateLargeJsonWorksNormally() {
    JsonNode firstSavedState = createNestedJsonNode(1);
    statefulExecutor.execute(
        "firstStateSaver", "testPartitionKey", "testSortKey", initialJsonState -> firstSavedState);

    JsonNode secondSavedState = createNestedJsonNode(2);
    AtomicReference<JsonNode> firstLoadedStateReference = new AtomicReference<>();
    statefulExecutor.execute(
        "firstStateLoader",
        "testPartitionKey",
        "testSortKey",
        initialJsonState -> {
          firstLoadedStateReference.set(initialJsonState);
          return secondSavedState;
        });
    assertEquals(firstSavedState, firstLoadedStateReference.get());

    AtomicReference<JsonNode> secondLoadedStateReference = new AtomicReference<>();
    statefulExecutor.execute(
        "secondStateLoader",
        "testPartitionKey",
        "testSortKey",
        initialJsonState -> {
          secondLoadedStateReference.set(initialJsonState);
          return OBJECT_MAPPER.createObjectNode();
        });
    assertEquals(secondSavedState, secondLoadedStateReference.get());
  }

  private static JsonNode createNestedJsonNode(int levels) {
    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    Stream.of("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        .forEach(
            key ->
                node.set(
                    key,
                    levels == 0
                        ? OBJECT_MAPPER.valueToTree(key)
                        : createNestedJsonNode(levels - 1)));
    return node;
  }
}
