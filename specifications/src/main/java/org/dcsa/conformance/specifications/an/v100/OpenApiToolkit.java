package org.dcsa.conformance.specifications.an.v100;

import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.SneakyThrows;
import org.dcsa.conformance.specifications.an.v100.constraints.SchemaConstraint;

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

  @SneakyThrows
  public static Field getClassField(Class<?> classObject, String fieldName) {
    return classObject.getDeclaredField(fieldName);
  }

  public static List<SchemaConstraint> getClassConstraints(String className) {
    try {
      return getClassConstraints(Class.forName(className));
    } catch (ClassNotFoundException e) {
      return Collections.EMPTY_LIST;
    }
  }

  public static List<SchemaConstraint> getClassConstraints(Class<?> classObject) {
    Method getConstraintsMethod;
    try {
      getConstraintsMethod = classObject.getMethod("getConstraints");
    } catch (NoSuchMethodException e) {
      return Collections.EMPTY_LIST;
    }
    Object invocationResult;
    try {
      invocationResult = getConstraintsMethod.invoke(null);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
    return (List<SchemaConstraint>) invocationResult;
  }
}
