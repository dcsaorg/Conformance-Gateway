package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.check.KeywordDatasets.*;

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
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, SELECT_FIRST_COLUMN));
  }

  static KeywordDataset fromCSV(Class<?> resourceClass, String resourceName, String columnName) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, SelectColumn.withName(columnName)));
  }

  static KeywordDataset fromCSVCombingColumns(Class<?> resourceClass, String resourceName, String delimiter, String ... columnNames) {
    checkResource(resourceClass, resourceName);
    return KeywordDataset.lazyLoaded(() -> loadCsvDataset(resourceClass, resourceName, new CombineColumnSelector(delimiter, columnNames)));
  }
}
