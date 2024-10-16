package org.dcsa.conformance.core.check;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

class KeywordDatasets {

  private static final String SPLIT_CHAR = ",";

  private KeywordDatasets() {}

  static void checkResource(@NonNull String resourceName) {
    var resource = KeywordDatasets.class.getResource(resourceName);
    if (resource == null) {
      throw new IllegalArgumentException("Could not find the resource: " + resourceName);
    }
  }

  static final CSVRowSelector SELECT_FIRST_COLUMN = (resourceName, row) -> {
    if (row.length < 1) {
      throw new IllegalArgumentException("Empty lines are not allowed; found one in " + resourceName);
    }
    return row[0];
  };

  /**
   * Load a CSV file as a dataset of keywords. File must have a header line and split by {@link
   * #SPLIT_CHAR} Note 1: it currently does not support escaping or quoting of values. Note 2:
   * Values should not contain the split char.
   *
   * @param resourceName File path to the CSV file
   * @param selector Selector for the column to use as the keyword
   * @return A dataset of keywords
   */
  @SneakyThrows
  static KeywordDataset loadCsvDataset(
      @NonNull String resourceName, @NonNull CSVRowSelector selector) {
    // Can not use Files.lines() as it does not work with resources in JAR files
    var lines = new ArrayList<String>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(KeywordDatasets.class.getResourceAsStream(resourceName))))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }
    String[] headers = lines.getFirst().split(SPLIT_CHAR);
    selector.setup(resourceName, headers);

    Set<String> keywords = HashSet.newHashSet(lines.size());
    lines.stream()
        .skip(1) // Skip header line
        .map(line -> verifyAndSplitLine(line, headers.length))
        .map(row -> selector.selectValue(resourceName, row))
        .forEach(keywords::add);
    return Collections.unmodifiableSet(keywords)::contains;
  }

  private static String[] verifyAndSplitLine(String line, int length) {
    if (line.isBlank()) {
      return new String[0];
    }
    String[] split = line.split(SPLIT_CHAR);
    if (split.length <= length) return split;
    throw new IllegalArgumentException(
        "CSV line has wrong number of columns or contains split char in values: " + line);
  }

  interface CSVRowSelector {
    default void setup(String resourceName, String[] headers) {}

    String selectValue(String resourceName, String[] row);
  }

  @RequiredArgsConstructor(staticName = "withName")
  static class SelectColumn implements CSVRowSelector {
    private final String columnName;
    private int columnOffset = -1;

    @Override
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

    @Override
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
