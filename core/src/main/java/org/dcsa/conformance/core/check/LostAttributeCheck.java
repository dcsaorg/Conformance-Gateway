package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.core.check.JsonAttribute.renderValue;

@RequiredArgsConstructor(staticName = "of")
public class LostAttributeCheck implements JsonRebaseableContentCheck {

  @Getter
  private final String description;
  private final Supplier<JsonNode> baseNode;
  private final BiConsumer<JsonNode, JsonNode> normalizer;


  private void checkNode(JsonNode leftNode, JsonNode rightNode, String contextPath, Set<String> issues) {
    if (leftNode.isObject()) {
      var fields = leftNode.fields();
      while (fields.hasNext()) {
        var entry = fields.next();
        var attrName = entry.getKey();
        var leftValueNode = entry.getValue();
        var rightValueNode = rightNode.path(attrName);
        var path = concatContextPath(contextPath, attrName);
        this.checkNode(leftValueNode, rightValueNode, path, issues);
      }
    } else if (leftNode.isArray()) {
      int index = 0;
      for (JsonNode leftChild : leftNode) {
        var path = contextPath + "[" + index + "]";
        var rightChild = rightNode.path(index);
        ++index;
        this.checkNode(leftChild, rightChild, path, issues);
      }
    } else if (!leftNode.equals(rightNode)) {
      issues.add("The attribute '%s' was '%s' but should have been '%s'".formatted(
        contextPath,
        renderValue(rightNode),
        renderValue(leftNode))
      );
    }
  }

  @Override
  public Set<String> validate(JsonNode nodeToValidate, String contextPath) {
    var left = baseNode.get();
    var right = nodeToValidate;
    if (left == null) {
      return Set.of();
    }
    var normalizer = this.normalizer;
    if (normalizer != null) {
      left = left.deepCopy();
      right = right.deepCopy();
      this.normalizer.accept(left, right);
    }
    var issues = new LinkedHashSet<String>();
    this.checkNode(left, right, contextPath, issues);
    return issues;
  }


  @Override
  public String description() {
    return this.description;
  }
}
