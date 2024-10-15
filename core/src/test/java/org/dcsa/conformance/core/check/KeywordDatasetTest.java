package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeywordDatasetTest {

  @Test
  void convertCSVFileTo() {
    KeywordDataset code =
        KeywordDataset.fromCSV("/convert-csv-file.csv", "General Reference Type Code");
    assertTrue(code.contains("SAC"));
    assertTrue(code.contains("BID"));
  }
}
