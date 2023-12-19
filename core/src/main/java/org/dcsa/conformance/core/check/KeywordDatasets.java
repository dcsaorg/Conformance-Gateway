package org.dcsa.conformance.core.check;

import com.opencsv.CSVReader;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

class KeywordDatasets {
  private KeywordDatasets() {}


  static void checkResource(Class<?> resourceClass, String resourceName) {
    var resource = resourceClass.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Could not find the resource " + resourceName
        + " via " + resourceClass.getSimpleName());
    }
  }

  @SneakyThrows
  static KeywordDataset loadCsvDataset(Class<?> resourceClass, String resourceName, String columnName) {
    Set<String> keywords = new HashSet<>();
    int columnOffset = 0;
    try (var reader = new CSVReader(new BufferedReader(new InputStreamReader(Objects.requireNonNull(resourceClass.getResourceAsStream(resourceName)))))) {
      var headers = reader.readNext();
      if (columnName != null) {
        columnOffset = Arrays.asList(headers).indexOf(columnName);
        if (columnOffset < 0) {
          throw new IllegalArgumentException("Could not find column \"%s\" in %s".formatted(columnName, resourceName));
        }
      }
      for (var line : reader) {
        if (line.length <= columnOffset) {
          if (columnName == null) {
            throw new IllegalArgumentException("Empty lines are not allowed; found one in " + resourceName);
          }
          throw new IllegalArgumentException("Some lines in %s were too short and did not cover column %s".formatted(resourceName, columnName));
        }
        var value = line[columnOffset];
        keywords.add(value);
      }
    }
    return Collections.unmodifiableSet(keywords)::contains;
  }
}
