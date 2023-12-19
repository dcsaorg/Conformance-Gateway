package org.dcsa.conformance.core.check;

import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

class KeywordDatasets {
  private KeywordDatasets() {}


  static void checkResource(Class<?> resourceClass, String resourceName) {
    var resource = resourceClass.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Could not find the resource " + resourceName
        + " via " + resourceClass.getSimpleName());
    }
  }

  static final CSVRowSelector SELECT_FIRST_COLUMN = (resourceName, row) -> {
    if (row.length < 1) {
      throw new IllegalArgumentException("Empty lines are not allowed; found one in " + resourceName);
    }
    return row[0];
  };


  @SneakyThrows
  static KeywordDataset loadCsvDataset(Class<?> resourceClass, String resourceName, CSVRowSelector selector) {
    Set<String> keywords = new HashSet<>();
    try (var reader = new CSVReader(new BufferedReader(new InputStreamReader(Objects.requireNonNull(resourceClass.getResourceAsStream(resourceName)))))) {
      var headers = reader.readNext();
      selector.setup(resourceName, headers);
      for (var line : reader) {
        var value = selector.selectValue(resourceName, line);
        keywords.add(value);
      }
    }
    return Collections.unmodifiableSet(keywords)::contains;
  }


  interface CSVRowSelector {
    default void setup(String resourceName, String[] headers) {
    }

    String selectValue(String resourceName, String[] row);
  }

  @RequiredArgsConstructor(staticName = "withName")
  static class SelectColumn implements CSVRowSelector {
    private final String columnName;
    private int columnOffset = -1;

    public void setup(String resourceName, String[] headers) {
      var offset = Arrays.asList(headers).indexOf(columnName);
      if (offset < 0) {
        throw new IllegalArgumentException("Could not find column \"%s\" in %s".formatted(columnName, resourceName));
      }
      this.columnOffset = offset;
    }

    public String selectValue(String resourceName, String[] row) {
      if (row.length <= columnOffset) {
        throw new IllegalArgumentException("Some lines in %s were too short and did not cover column %s".formatted(resourceName, columnName));
      }
      return row[columnOffset];
    }
  }

  static class CombineColumnSelector implements CSVRowSelector {
    private final String delimiter;
    private final String[] columns;
    private int[] indexes = null;

    CombineColumnSelector(String delimiter, String ... columns) {
      this.delimiter = delimiter;
      this.columns = columns;
      if (columns.length < 2) {
        throw new IllegalArgumentException("Must combine at least two columns");
      }
    }

    public void setup(String resourceName, String[] headers) {
      var headersAsList = Arrays.asList(headers);
      this.indexes = Arrays.stream(columns).mapToInt(columnName -> {
        var offset = headersAsList.indexOf(columnName);
        if (offset < 0) {
          throw new IllegalArgumentException("Could not find column \"%s\" in %s".formatted(columnName, resourceName));
        }
        return offset;
      }).toArray();
    }

    public String selectValue(String resourceName, String[] row) {
      return Arrays.stream(indexes)
        .mapToObj(offset -> {
          if (row.length <= offset) {
            throw new IllegalArgumentException("Some lines in %s were too short and did not cover all the columns".formatted(resourceName));
          }
          return row[offset];
        }).collect(Collectors.joining(delimiter));
    }
  }
}
