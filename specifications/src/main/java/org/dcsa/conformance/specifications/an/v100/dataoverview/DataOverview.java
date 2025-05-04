package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.io.FileOutputStream;
import java.util.List;

import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DataOverview {

  private final QueryParametersSheet queryParametersSheet;

  public DataOverview(List<Parameter> queryParameters) {
    queryParametersSheet = new QueryParametersSheet(queryParameters);
  }

  @SneakyThrows
  public void exportToExcelFile(String excelFilePath) {
    try (Workbook workbook = new XSSFWorkbook()) {
      queryParametersSheet.addToExcelWorkbook(workbook);
      try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
        workbook.write(fileOut);
      }
    }
  }
}
