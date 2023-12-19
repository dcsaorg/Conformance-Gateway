package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.check.KeywordDatasets.checkResource;
import static org.dcsa.conformance.core.check.KeywordDatasets.loadCsvDataset;

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

  static KeywordDataset fromCSV(Class<?> resourceClass, String resourceName) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, null));
  }

  static KeywordDataset fromCSV(Class<?> resourceClass, String resourceName, String columnName) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, columnName));
  }
}
