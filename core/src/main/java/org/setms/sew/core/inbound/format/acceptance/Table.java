package org.setms.sew.core.inbound.format.acceptance;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class Table {

  private final List<String> headers;
  private final List<List<String>> rows = new ArrayList<>();

  Table(String... headers) {
    this(Arrays.asList(headers));
  }

  void addRow(String... row) {
    addRow(Arrays.asList(row));
  }

  void addRow(List<String> row) {
    rows.add(row);
  }

  void printTo(PrintWriter writer) {
    var columnWidths = calculateColumnWidths();

    printRow(headers, columnWidths, writer);
    printSeparatorRow(writer, columnWidths);
    for (var row : rows) {
      printRow(row, columnWidths, writer);
    }
  }

  private int[] calculateColumnWidths() {
    var result = new int[headers.size()];
    for (var i = 0; i < headers.size(); i++) {
      result[i] = headers.get(i).length();
    }
    for (var row : rows) {
      for (var i = 0; i < row.size(); i++) {
        result[i] = Math.max(result[i], row.get(i).length());
      }
    }
    return result;
  }

  private void printRow(List<String> row, int[] widths, PrintWriter writer) {
    writer.print("|");
    for (int i = 0; i < row.size(); i++) {
      writer.printf(" %-" + widths[i] + "s |", row.get(i));
    }
    writer.println();
  }

  private void printSeparatorRow(PrintWriter writer, int[] columnWidths) {
    writer.print("|");
    for (var width : columnWidths) {
      writer.print("-".repeat(width + 2));
      writer.print("|");
    }
    writer.println();
  }
}
