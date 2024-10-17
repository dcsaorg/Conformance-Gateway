package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ToStringComparator implements Comparator<JsonNode> {

  public static final ToStringComparator INSTANCE = new ToStringComparator();

  @Override
  public int compare(JsonNode o1, JsonNode o2) {
    var compared = o1.getNodeType().compareTo(o2.getNodeType());
    if (compared != 0) return compared;

    var o1sb = new StringBuilder();
    var o2sb = new StringBuilder();
    stringify(o1sb, o1);
    stringify(o2sb, o2);
    return o1.toString().compareTo(o2.toString());
  }

  private static void stringify(StringBuilder builder, JsonNode node) {
    switch (node.getNodeType()) {
      case ARRAY -> {
        builder.append("[");
        for (int i = 0; i < node.size(); i++) {
          stringify(builder, node.get(i));
        }
        builder.append("]");
      }
      case OBJECT -> {
        builder.append("{");
        var nameIterator = node.fieldNames();
        var names = new ArrayList<String>();
        while (nameIterator.hasNext()) {
          names.add(nameIterator.next());
        }
        names.sort(Comparator.naturalOrder());
        boolean first = true;
        for (var name : names) {
          if (first) {
            first = false;
          } else {
            builder.append(",");
          }
          var value = node.get(name);
          builder.append("\"").append(name).append("\": ");
          stringify(builder, value);
        }
        builder.append("}");
      }
      default -> builder.append(node);
    }
  }
}
