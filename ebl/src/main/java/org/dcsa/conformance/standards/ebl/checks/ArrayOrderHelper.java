package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ArrayOrderHelper {


  public static JsonNode restoreArrayOrder(JsonNode containerNode, String attribute, ArrayOrderHandler arrayOrderHandler) {
    return applyArrayOrder(containerNode, attribute, arrayOrderHandler::restoreOrder);
  }

  public static JsonNode shuffleArrayOrder(JsonNode containerNode, String attribute, ArrayOrderHandler arrayOrderHandler) {
    return applyArrayOrder(containerNode, attribute, arrayOrderHandler::shuffle);
  }

  private static JsonNode applyArrayOrder(JsonNode containerNode, String attribute, Function<ArrayNode, ArrayNode> handler) {
    if (!containerNode.isObject()) {
      return containerNode;
    }
    var objectNode = (ObjectNode)containerNode;
    var arrayNode = objectNode.path(attribute);
    if (arrayNode.isArray()) {
      objectNode.set(attribute, handler.apply((ArrayNode) arrayNode));
    }
    return objectNode;
  }
}
