package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.check.KeywordDatasets.*;

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

  static KeywordDataset fromCSV(Class<?> resourceClass, String resourceName) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, SELECT_FIRST_COLUMN));
  }

  static KeywordDataset fromCSV(Class<?> resourceClass, String resourceName, String columnName) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, SelectColumn.withName(columnName)));
  }

  static KeywordDataset fromVersionedCSV(Class<?> resourceClass, String nameTemplate, String columnName) {
    return VersionedKeywordDataset.of(resourceClass, nameTemplate, SelectColumn.withName(columnName));
  }

  static KeywordDataset fromVersionedCSV(Class<?> resourceClass, String nameTemplate, String delimiter, String ... columnNames) {
    return VersionedKeywordDataset.of(resourceClass, nameTemplate, new CombineColumnSelector(delimiter, columnNames));
  }
}
