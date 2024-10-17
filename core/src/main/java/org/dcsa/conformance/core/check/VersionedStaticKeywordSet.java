package org.dcsa.conformance.core.check;


import java.util.Set;
import java.util.function.Predicate;

public interface VersionedStaticKeywordSet {

  Set<String> keywordsFor(String standardsVersion);

  static VersionedStaticKeywordSet versionedKeywords(
    Predicate<String> versionGuard,
    Set<String> thenKeywords,
    Set<String> elseKeywords
  ) {
    if (thenKeywords == null || thenKeywords.isEmpty()) {
      throw new IllegalArgumentException("thenKeywords must have at least one keyword");
    }
    if (elseKeywords == null || elseKeywords.isEmpty()) {
      throw new IllegalArgumentException("elseKeywords must have at least one keyword");
    }
    var thenCopy = Set.copyOf(thenKeywords);
    var elseCopy = Set.copyOf(elseKeywords);
    return standardsVersion -> versionGuard.test(standardsVersion) ? thenCopy : elseCopy;
  }
}
