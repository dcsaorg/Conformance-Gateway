package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STTableType;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class DataOverviewSheet {
  private final String sheetName;
  private final String tableName;
  private final List<String> headerTitles;
  private final List<Integer> columnWidths;
  private final List<Boolean> wrapCellText;
  private final List<List<String>> dataValues;

  public void addToExcelWorkbook(Workbook workbook, Supplier<Long> idSupplier) {
    Sheet sheet = workbook.createSheet(sheetName);

    XSSFCellStyle wrapStyle = (XSSFCellStyle) workbook.createCellStyle();
    wrapStyle.setWrapText(true);
    ArrayList<Boolean> doWrapCellText = new ArrayList<>(wrapCellText);

    AtomicInteger rowIndexReference = new AtomicInteger(0);
    Stream.concat(Stream.of(headerTitles), dataValues.stream())
        .forEach(
            rowValues -> {
              int rowIndex = rowIndexReference.getAndIncrement();
              Row row = sheet.createRow(rowIndex);
              row.setHeight((short) -1);
              AtomicInteger columnIndexReference = new AtomicInteger(0);
              rowValues.forEach(
                  value -> {
                    int columnIndex = columnIndexReference.getAndIncrement();
                    Cell cell = row.createCell(columnIndex);
                    cell.setCellValue(value);
                    if (rowIndex > 0 && doWrapCellText.get(columnIndex)) {
                      cell.setCellStyle(wrapStyle);
                    }
                  });
            });
    AtomicInteger columnIndexReference = new AtomicInteger(0);
    columnWidths.forEach(
        width -> {
          int columnIndex = columnIndexReference.getAndIncrement();
          if (width > 0) {
            sheet.setColumnWidth(columnIndex, 256 * width);
          } else {
            sheet.autoSizeColumn(columnIndex);
          }
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
    ctTable.setId(idSupplier.get());
    ctTable.setTotalsRowShown(false);
    ctTable.setTotalsRowCount(0L);
    ctTable.setRef(tableRange);
    ctTable.setTableType(STTableType.WORKSHEET);

    CTTableColumns columns = ctTable.addNewTableColumns();
    columns.setCount(headerTitles.size());
    headerTitles.forEach(
        title -> {
          CTTableColumn tableColumn = columns.addNewTableColumn();
          tableColumn.setId(idSupplier.get());
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
