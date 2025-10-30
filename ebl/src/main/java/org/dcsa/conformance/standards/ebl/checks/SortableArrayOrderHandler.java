package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor(staticName = "of")
public class SortableArrayOrderHandler implements ArrayOrderHandler {

  static final ArrayOrderHandler TO_STRING_SORTABLE_ARRAY_ORDER = of(ToStringComparator.INSTANCE);

  private final Comparator<JsonNode> comparator;

  @Override
  public ArrayNode restoreOrder(ArrayNode array) {
    var modifiableList =
        StreamSupport.stream(array.spliterator(), false).sorted(comparator).toList();
    var copy = array.deepCopy();
    copy.removeAll();
    copy.addAll(modifiableList);
    return copy;
  }

  @Override
  public ArrayNode shuffle(ArrayNode array) {
    var modifiableList =
        StreamSupport.stream(array.spliterator(), false).collect(Collectors.toList());
    Collections.shuffle(modifiableList);
    var copy = array.deepCopy();
    copy.removeAll();
    copy.addAll(modifiableList);
    return copy;
  }
}
