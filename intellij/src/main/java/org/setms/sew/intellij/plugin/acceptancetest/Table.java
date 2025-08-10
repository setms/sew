package org.setms.sew.intellij.plugin.acceptancetest;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.intellij.openapi.editor.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

record Table(
    Document document,
    int startLine,
    int endLine,
    int rowIndex,
    int columnIndex,
    List<List<String>> cells) {

  public static Table from(Document document, int offset) {
    var lineNr = document.getLineNumber(offset);
    var startLineNr = findTableStartLine(document, lineNr);
    var endLineNr = findTableEndLine(document, lineNr);
    if (startLineNr == endLineNr && getLine(document, startLineNr).isBlank()) {
      return null;
    }
    var lineTillCursor = document.getText().substring(document.getLineStartOffset(lineNr), offset);
    var columnIndex = parseRow(lineTillCursor).size() - 1;
    var cells = parseCells(document, startLineNr, endLineNr);
    return new Table(document, startLineNr, endLineNr, lineNr - startLineNr, columnIndex, cells);
  }

  private static int findTableStartLine(Document document, int lineNr) {
    var result = lineNr;
    while (result >= 0 && !getLine(document, result).isEmpty()) {
      result--;
    }
    if (result != 0) {
      result++;
    }
    return result;
  }

  private static String getLine(Document document, int line) {
    int start = document.getLineStartOffset(line);
    int end = document.getLineEndOffset(line);
    return document.getText().substring(start, end);
  }

  private static int findTableEndLine(Document document, int lineNr) {
    var result = lineNr;
    while (result < document.getLineCount() && !getLine(document, result).isEmpty()) {
      result++;
    }
    if (result > lineNr) {
      result--;
    }
    return result;
  }

  private static List<String> parseRow(String line) {
    var content = line.trim();
    if (line.isEmpty()) {
      return emptyList();
    }
    if (content.startsWith("|")) {
      content = content.substring(1);
    }
    if (content.endsWith("|")) {
      content = content.substring(0, content.length() - 1);
    }
    return Arrays.stream(content.split("\\|", -1)).map(String::trim).collect(toList());
  }

  private static ArrayList<List<String>> parseCells(
      Document document, int startLineNr, int endLineNr) {
    var result = new ArrayList<List<String>>();
    for (var i = startLineNr; i <= endLineNr; i++) {
      var line = getLine(document, i);
      result.add(parseRow(line));
    }
    return result;
  }

  public Delta addText(String text) {
    if (text.equals("|") && columnIndex >= 0) {
      return autoSizeCell();
    }
    return null;
  }

  public Delta autoSizeCell() {
    var row = cells.get(rowIndex);
    var columnWidth = maxWidth(columnIndex);
    var currentWidth = row.get(columnIndex).length();
    if (currentWidth >= columnWidth && columnIndex != row.size() - 1) {
      return null;
    }

    padCurrentCellTo(columnWidth);
    var updatedLine = renderRow(row);
    if (columnIndex == numColumns() - 1) {
      updatedLine += System.lineSeparator();
    }
    var lineNr = startLine + rowIndex;
    var start = document.getLineStartOffset(lineNr);
    var end = document.getLineEndOffset(lineNr);
    var caret = start + updatedLine.length();

    return new Delta(start, end, updatedLine, caret);
  }

  private int maxWidth(int column) {
    return cells.stream().map(row -> row.get(column)).mapToInt(String::length).max().orElse(0);
  }

  private void padCurrentCellTo(int width) {
    var row = cells.get(rowIndex);
    var content = row.get(columnIndex);
    row.set(columnIndex, padRight(content, width));
  }

  private String padRight(String text, int width) {
    return text + " ".repeat(width - text.length());
  }

  private String renderRow(List<String> cells) {
    var result = new StringBuilder("| ");
    for (var i = 0; i < cells.size(); i++) {
      result.append(padRight(cells.get(i), maxWidth(i))).append(" | ");
    }
    return result.toString().trim();
  }

  private int numColumns() {
    return cells.stream().mapToInt(Collection::size).max().orElse(0);
  }

  @Override
  public String toString() {
    return document
        .getText()
        .substring(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine));
  }
}
