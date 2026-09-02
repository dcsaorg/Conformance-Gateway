package org.dcsa.conformance.core.util;

import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.HashMap;
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

  /**
   * Merges scenario modules for the selected roles without silently replacing equally named modules.
   * Labels that occur for multiple roles are qualified with the role name; unique labels are kept as-is.
   */
  public static <V> LinkedHashMap<String, V> mergePartyScenarioModules(
      Map<String, Map<String, V>> modulesByRole, Collection<String> selectedRoles) {
    Map<String, Long> labelCounts =
        selectedRoles.stream()
            .flatMap(role -> modulesByRole.getOrDefault(role, Map.of()).keySet().stream())
            .collect(Collectors.groupingBy(label -> label, HashMap::new, Collectors.counting()));

    LinkedHashMap<String, V> mergedModules = new LinkedHashMap<>();
    selectedRoles.forEach(
        role ->
            modulesByRole
                .getOrDefault(role, Map.of())
                .forEach(
                    (label, builder) -> {
                      String mergedLabel = labelCounts.get(label) > 1 ? role + ": " + label : label;
                      if (mergedModules.putIfAbsent(mergedLabel, builder) != null) {
                        throw new IllegalArgumentException(
                            "Duplicate scenario module label after role qualification: " + mergedLabel);
                      }
                    }));
    return mergedModules;
  }
}
