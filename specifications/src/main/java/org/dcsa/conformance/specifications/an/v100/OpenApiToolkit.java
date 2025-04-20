package org.dcsa.conformance.specifications.an.v100;

import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public enum OpenApiToolkit {
  ; // no instances

  public static Map<String, Schema<?>> parameterizeStringRawSchemaMap(
      Map<String, Schema> rawSchemaMap) {
    TreeMap<String, Schema<?>> stringSchemaTreeMap = new TreeMap<>();
    if (rawSchemaMap != null) {
      rawSchemaMap.forEach(stringSchemaTreeMap::put);
    }
    return stringSchemaTreeMap;
  }

  public static List<Schema<?>> parameterizeRawSchemaList(List<Schema> rawSchemaList) {
    return (ArrayList) new ArrayList<>(rawSchemaList);
  }
}
