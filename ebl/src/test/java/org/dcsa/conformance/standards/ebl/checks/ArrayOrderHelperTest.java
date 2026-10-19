package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArrayOrderHelperTest {
  @Test
  void arrayHandlersRestoreSortOrderWithoutMutatingInput() {
    var array = OBJECT_MAPPER.createArrayNode().add("z").add("a");
    for (ArrayOrderHandler handler :
        List.of(
            ArrayOrderHandler.toStringSortableArray(),
            ArrayOrderHandler.sortableArray(Comparator.comparing(JsonNode::asText)))) {
      assertEquals("a", handler.restoreOrder(array).get(0).asText());
      assertEquals("z", array.get(0).asText());
      assertEquals(handler.restoreOrder(array), handler.restoreOrder(handler.shuffle(array)));
    }
    var preserve = ArrayOrderHandler.inputPreservedArrayOrder();
    assertSame(array, preserve.restoreOrder(array));
    assertSame(array, preserve.shuffle(array));
    ObjectNode root = OBJECT_MAPPER.createObjectNode();
    root.set("items", array);
    assertSame(
        root,
        ArrayOrderHelper.restoreArrayOrder(
            root, "items", ArrayOrderHandler.toStringSortableArray()));
    assertEquals("a", root.path("items").path(0).asText());
    assertSame(root, ArrayOrderHelper.shuffleArrayOrder(root, "items", preserve));
    assertSame(root, ArrayOrderHelper.restoreArrayOrder(root, "missing", preserve));
    assertSame(array, ArrayOrderHelper.restoreArrayOrder(array, "items", preserve));
  }
}
