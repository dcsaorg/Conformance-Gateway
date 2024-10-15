package org.dcsa.conformance.core.check;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

class KeywordDatasets {
  private KeywordDatasets() {}

  static Path getAndCheckResource(@NonNull String resourceName) {
    try {
      Path resourcePath = Paths.get(KeywordDatasets.class.getResource(resourceName).toURI());
      if (!Files.exists(resourcePath)) {
        throw new IllegalArgumentException("Could not find the resource: " + resourceName);
      }
      return resourcePath;
    } catch (NullPointerException | URISyntaxException e) {
      throw new IllegalArgumentException("Could not find the resource: " + resourceName);
    }
  }

  static final CSVRowSelector SELECT_FIRST_COLUMN = (resourceName, row) -> {
    if (row.length < 1) {
      throw new IllegalArgumentException("Empty lines are not allowed; found one in " + resourceName);
    }
    return row[0];
  };

  @SneakyThrows
  static KeywordDataset loadCsvDataset(@NonNull Path resource, CSVRowSelector selector) {
    List<String> lines = Files.readAllLines(resource);
    String[] headers = lines.getFirst().split(",");
    String fileName = String.valueOf(resource.getFileName());
    selector.setup(fileName, headers);

    Set<String> keywords = HashSet.newHashSet(lines.size());
    lines.stream()
        .map(line -> line.split(","))
        .map(row -> selector.selectValue(fileName, row))
        .forEach(keywords::add);
    return Collections.unmodifiableSet(keywords)::contains;
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
