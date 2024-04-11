package org.dcsa.conformance.core.check;

import static org.dcsa.conformance.core.check.KeywordDatasets.loadCsvDataset;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;

class VersionedKeywordDataset implements KeywordDataset {

  static final ThreadLocal<String> STANDARD_VERSION_BEING_CHECKED = new ThreadLocal<>();

  private final Function<String, KeywordDataset> datasetLoader;
  private final ConcurrentHashMap<String, KeywordDataset> datasets = new ConcurrentHashMap<>();

  private VersionedKeywordDataset(@NonNull Function<String, KeywordDataset> datasetLoader) {
    this.datasetLoader = datasetLoader;
  }

  @Override
  public boolean contains(String value) {
    return getDataset().contains(value);
  }

  private KeywordDataset getDataset() {
    var version = STANDARD_VERSION_BEING_CHECKED.get();
    if (version == null) {
      throw new IllegalStateException("Standard version not available");
    }
    return datasets.computeIfAbsent(version, datasetLoader);
  }

  static <T> T withVersion(String version, Supplier<T> code) {
    try {
      STANDARD_VERSION_BEING_CHECKED.set(version);
      return code.get();
    } finally {
      STANDARD_VERSION_BEING_CHECKED.remove();
    }
  }

  static KeywordDataset fromLoader(Function<String, KeywordDataset> datasetLoader) {
    return new VersionedKeywordDataset(datasetLoader);
  }

  static KeywordDataset of(Class<?> resourceClass, String nameTemplate, KeywordDatasets.CSVRowSelector rowSelector) {
    if (!nameTemplate.contains("%s")) {
      throw new IllegalStateException("Missing a '%s' to mark where the version will be placed");
    }
    return fromLoader((version) -> {
      var resourceName = nameTemplate.formatted(version);
      return loadCsvDataset(resourceClass, resourceName, rowSelector);
    });
  }
}
