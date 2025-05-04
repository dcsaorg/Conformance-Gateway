package org.dcsa.conformance.specifications.an.v100.dataoverview;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STTableType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class DataOverviewSheet {
  private final String sheetName;
  private final String tableName;
  private final List<String> headerTitles;
  private final List<List<String>> dataValues;

  public void addToExcelWorkbook(Workbook workbook) {
    Sheet sheet = workbook.createSheet(sheetName);

    AtomicInteger rowIndex = new AtomicInteger(0);
    Stream.concat(Stream.of(headerTitles), dataValues.stream())
        .forEach(
            rowValues -> {
              Row row = sheet.createRow(rowIndex.getAndIncrement());
              AtomicInteger columnIndex = new AtomicInteger(0);
              rowValues.forEach(
                  value -> row.createCell(columnIndex.getAndIncrement()).setCellValue(value));
            });

    XSSFSheet xssfSheet = (XSSFSheet) sheet;
    XSSFTable table = xssfSheet.createTable(null);

    String tableRange =
        "A1:%s%d"
            .formatted(
                CellReference.convertNumToColString(headerTitles.size() - 1),
                1 + dataValues.size());

    CTTable ctTable = table.getCTTable();
    ctTable.setDisplayName(tableName); // does not support spaces
    ctTable.setName(tableName);
    ctTable.setId(1L);
    ctTable.setTotalsRowShown(false);
    ctTable.setTotalsRowCount(0L);
    ctTable.setRef(tableRange);
    ctTable.setTableType(STTableType.WORKSHEET);

    CTTableColumns columns = ctTable.addNewTableColumns();
    columns.setCount(headerTitles.size());
    AtomicLong nextColumnId = new AtomicLong(1L);
    headerTitles.forEach(
        title -> {
          CTTableColumn tableColumn = columns.addNewTableColumn();
          tableColumn.setId(nextColumnId.getAndIncrement());
          tableColumn.setName(title);
        });

    CTTableStyleInfo styleInfo = ctTable.addNewTableStyleInfo();
    styleInfo.setName("TableStyleMedium2");
    styleInfo.setShowColumnStripes(false);
    styleInfo.setShowRowStripes(true);

    CTAutoFilter autoFilter = ctTable.addNewAutoFilter();
    autoFilter.setRef(tableRange);
  }
}
