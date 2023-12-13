package org.dcsa.conformance.core.check;

import java.util.Set;
import java.util.function.Supplier;

@FunctionalInterface
public interface KeywordDataset {

  boolean contains(String value);

  static KeywordDataset lazyLoaded(Supplier<KeywordDataset> loader) {
    return new LazyKeywordDataset(loader);
  }

  static KeywordDataset staticDataset(String... values) {
    return Set.of(values)::contains;
  }
}
