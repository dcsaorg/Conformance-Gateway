package org.dcsa.conformance.specifications.an.v100.dataoverview;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNotice;
import org.dcsa.conformance.specifications.an.v100.model.ArrivalNoticeNotification;

@Slf4j
public class DataOverview {

  private final AttributesHierarchicalSheet attributesHierarchicalSheet;
  private final AttributesNormalizedSheet attributesNormalizedSheet;
  private final QueryParametersSheet queryParametersSheet;
  private final QueryFiltersSheet queryFiltersSheet;

  public DataOverview(
      OpenAPI openAPI,
      List<Parameter> queryParameters,
      Map<Boolean, List<List<Parameter>>> requiredAndOptionalFilters) {
    AttributesData attributesData =
        new AttributesData(
            openAPI,
            List.of(
                ArrivalNotice.class.getSimpleName(),
                ArrivalNoticeNotification.class.getSimpleName()));
    attributesHierarchicalSheet = new AttributesHierarchicalSheet(attributesData);
    attributesNormalizedSheet = new AttributesNormalizedSheet(attributesData);
    queryParametersSheet = new QueryParametersSheet(queryParameters);
    queryFiltersSheet = new QueryFiltersSheet(requiredAndOptionalFilters);
  }

  @SneakyThrows
  public void exportToExcelFile(String excelFilePath) {
    try (Workbook workbook = new XSSFWorkbook()) {
      AtomicLong nextId = new AtomicLong(0);
      Stream.of(
              attributesHierarchicalSheet,
              attributesNormalizedSheet,
              queryParametersSheet,
              queryFiltersSheet)
          .forEach(sheet -> sheet.addToExcelWorkbook(workbook, nextId::incrementAndGet));
      try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
        workbook.write(fileOut);
      }
    }
    log.info("Data Overview exported to {}", excelFilePath);
  }

  public void exportToCsvFiles(String csvFilePattern) {
    Stream.of(
            attributesHierarchicalSheet,
            attributesNormalizedSheet,
            queryParametersSheet,
            queryFiltersSheet)
        .forEach(sheet -> sheet.exportToCsvFile(csvFilePattern));
  }
}
