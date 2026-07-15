package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordDatasetTest {

  @Test
  void staticDatasetExposesValuesInDeclarationOrder() {
    var dataset = KeywordDataset.staticDataset("FIRST", "SECOND", "THIRD");

    assertEquals(List.of("FIRST", "SECOND", "THIRD"), dataset.values().stream().toList());
  }

  @Test
  void convertCorrectCSVFileToKeywordDataset() {
    KeywordDataset code =
        KeywordDataset.fromCSV("/convert-csv-file.csv", "General Reference Type Code");
    assertTrue(code.contains("SAC"));
    assertTrue(code.contains("BID"));
    assertTrue(code.contains("FF"));
    assertFalse(code.contains("General Reference Type Code")); // Header should be skipped
    assertThrows(UnsupportedOperationException.class, code::values);
  }

  @Test
  void convertWrongCSVFile() {
    KeywordDataset code =
        KeywordDataset.fromCSV("/convert-csv-file_wrong.csv", "General Reference Type Code");
    assertThrows(IllegalArgumentException.class, () -> code.contains("SAC"));
  }
}
