package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class InputPreservingArrayOrderHandler implements ArrayOrderHandler {

  static final ArrayOrderHandler INSTANCE = new InputPreservingArrayOrderHandler();

  @Override
  public ArrayNode restoreOrder(ArrayNode array) {
    return array;
  }

  @Override
  public ArrayNode shuffle(ArrayNode array) {
    return array;
  }
}
