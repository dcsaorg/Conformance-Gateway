package org.dcsa.conformance.core.util;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class MapUtils {

  @SafeVarargs
  public static <K, V> Map<K, V> orderedMap(Map.Entry<K, V>... entries) {
    return Stream.of(entries)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
}
