package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.check.KeywordDatasets.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import lombok.NonNull;

@FunctionalInterface
public interface KeywordDataset {

  boolean contains(String value);

  static KeywordDataset lazyLoaded(Supplier<KeywordDataset> loader) {
    return new LazyKeywordDataset(loader);
  }

  static KeywordDataset staticDataset(String... values) {
    if (values.length < 2) {
      throw new IllegalStateException("A data set must be at least two values");
    }
    return Set.of(values)::contains;
  }

  static KeywordDataset staticVersionedDataset(@NonNull Object @NonNull ... values) {
    var anyInvalid = Arrays.stream(values)
      .anyMatch(x -> !(x instanceof String || x instanceof VersionedStaticKeywordSet));
    if (anyInvalid) {
      throw new IllegalStateException("Only (non-null) String and VersionedStaticKeywordSet allowed");
    }
    return VersionedKeywordDataset.fromLoader(
      version -> {
        var keywords = new HashSet<String>();

        for (var value : values) {
          if (value instanceof VersionedStaticKeywordSet vsks) {
            keywords.addAll(vsks.keywordsFor(version));
          } else if (value instanceof String v) {
            keywords.add(v);
          } else {
            throw new IllegalStateException("Could not resolve '%s' as a keyword".formatted(value));
          }
        }
        return Collections.unmodifiableSet(keywords)::contains;
      }
    );
  }

  static KeywordDataset fromCSV(String resourceName) {
    Path resource = getAndCheckResource(resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resource, SELECT_FIRST_COLUMN));
  }

  static KeywordDataset fromCSV(String resourceName, String columnName) {
    Path resource = getAndCheckResource(resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resource, SelectColumn.withName(columnName)));
  }

  static KeywordDataset fromCSVCombiningColumns(String resourceName, String delimiter, String ... columnNames) {
    Path resource = getAndCheckResource(resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resource, new CombineColumnSelector(delimiter, columnNames)));
  }

  static KeywordDataset fromVersionedCSV(String nameTemplate) {
    return VersionedKeywordDataset.of(nameTemplate, SELECT_FIRST_COLUMN);
  }

  static KeywordDataset fromVersionedCSV(String nameTemplate, String columnName) {
    return VersionedKeywordDataset.of(nameTemplate, SelectColumn.withName(columnName));
  }

  static KeywordDataset fromVersionedCSV(String nameTemplate, String delimiter, String ... columnNames) {
    return VersionedKeywordDataset.of(nameTemplate, new CombineColumnSelector(delimiter, columnNames));
  }
}
