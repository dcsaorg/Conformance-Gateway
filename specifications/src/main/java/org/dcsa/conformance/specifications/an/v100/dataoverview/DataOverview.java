package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DataOverview {

  private final QueryParametersSheet queryParametersSheet;
  private final QueryFiltersSheet queryFiltersSheet;

  public DataOverview(
      List<Parameter> queryParameters,
      Map<Boolean, List<List<Parameter>>> requiredAndOptionalFilters) {
    queryParametersSheet = new QueryParametersSheet(queryParameters);
    queryFiltersSheet = new QueryFiltersSheet(requiredAndOptionalFilters);
  }

  @SneakyThrows
  public void exportToExcelFile(String excelFilePath) {
    try (Workbook workbook = new XSSFWorkbook()) {
      AtomicLong nextId = new AtomicLong(0);
      queryParametersSheet.addToExcelWorkbook(workbook, nextId::incrementAndGet);
      queryFiltersSheet.addToExcelWorkbook(workbook, nextId::incrementAndGet);
      try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
        workbook.write(fileOut);
      }
    }
  }
}
