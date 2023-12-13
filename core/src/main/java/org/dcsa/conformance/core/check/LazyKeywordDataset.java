package org.dcsa.conformance.core.check;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import java.util.function.Supplier;

class LazyKeywordDataset implements KeywordDataset {
  private final Supplier<KeywordDataset> datasetLoader;

  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  private final KeywordDataset dataset = datasetLoader.get();

  LazyKeywordDataset(@NonNull Supplier<KeywordDataset> datasetLoader) {
    this.datasetLoader = datasetLoader;
  }

  @Override
  public boolean contains(String value) {
    return getDataset().contains(value);
  }
}
