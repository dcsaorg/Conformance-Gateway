package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Comparator;

public interface ArrayOrderHandler {
  ArrayNode restoreOrder(ArrayNode arrayNode);

  ArrayNode shuffle(ArrayNode array);

  static ArrayOrderHandler inputPreservedArrayOrder() {
    return InputPreservingArrayOrderHandler.INSTANCE;
  }

  static ArrayOrderHandler toStringSortableArray() {
    return SortableArrayOrderHandler.TO_STRING_SORTABLE_ARRAY_ORDER;
  }

  static ArrayOrderHandler sortableArray(Comparator<JsonNode> comparator) {
    return SortableArrayOrderHandler.of(comparator);
  }
}
