package org.setms.sew.intellij.toolwindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.setms.sew.core.domain.model.sdlc.process.Todo;

class TodoTableModel extends AbstractTableModel {

  private final List<Todo> items;

  public TodoTableModel(Collection<Todo> items) {
    this.items = items instanceof List<Todo> list ? list : new ArrayList<>(items);
  }

  @Override
  public int getRowCount() {
    return items.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(int column) {
    return switch (column) {
      case 0 -> "Location";
      case 1 -> "Task";
      default -> "";
    };
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    var todo = getItemAt(rowIndex);
    return switch (columnIndex) {
      case 0 -> todo.getLocation();
      case 1 -> todo.getAction();
      default -> "";
    };
  }

  public Todo getItemAt(int row) {
    return items.get(row);
  }
}
