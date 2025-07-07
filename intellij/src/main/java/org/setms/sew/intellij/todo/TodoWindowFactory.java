package org.setms.sew.intellij.todo;

import static java.util.Collections.emptyMap;
import static javax.swing.SwingUtilities.isLeftMouseButton;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.format.Files;
import org.setms.sew.core.domain.model.sdlc.process.Todo;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.inbound.format.sal.SalFormat;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;
import org.setms.sew.intellij.tool.ToolRunner;
import org.setms.sew.intellij.tool.VirtualFileInputSource;

public class TodoWindowFactory implements ToolWindowFactory, DumbAware {

  private static final String FILE_URI_SCHEME = "file:";

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    var hint = new JLabel("Double-click or press Alt+Enter to start a task.");
    hint.setFont(hint.getFont().deriveFont(Font.ITALIC, 10f));
    hint.setForeground(JBColor.GRAY);

    var urisByTodo = loadTodos(project);
    var tableModel = new TodoTableModel(urisByTodo.keySet());
    var table = new JBTable(tableModel);
    table.setShowGrid(false);
    table.setRowHeight(24);
    table.setStriped(true);
    var scrollPane = new JBScrollPane(table);

    var panel = new JPanel(new BorderLayout());
    panel.add(hint, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    var content = ContentFactory.getInstance().createContent(panel, "", false);
    toolWindow.getContentManager().addContent(content);

    performTodoOnDoubleMouseClick(project, table, urisByTodo);
    performTodoOnAltEnter(project, table, urisByTodo);
  }

  private Map<Todo, URI> loadTodos(Project project) {
    var result = new TreeMap<Todo, URI>();
    var parser = new SalFormat().newParser();
    toInputSource(project)
        .matching(new Glob("", "**/*.todo"))
        .forEach(
            source -> {
              var uri = source.toSink().toUri();
              try (var input = source.open()) {
                var todo = parser.parse(input, Todo.class, false);
                result.put(todo, uri);
              } catch (IOException e) {
                System.err.println(e.getMessage());
              }
            });
    return result;
  }

  private VirtualFileInputSource toInputSource(Project project) {
    return toInputSource(project.getBasePath());
  }

  private VirtualFileInputSource toInputSource(String path) {
    return new VirtualFileInputSource(
        LocalFileSystem.getInstance().findFileByPath(path), emptyMap(), ignored -> true);
  }

  private void performTodoOnDoubleMouseClick(
      Project project, JBTable table, Map<Todo, URI> urisByTodo) {
    table.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && isLeftMouseButton(e)) {
              perform(table, table.rowAtPoint(e.getPoint()), project, urisByTodo);
            }
          }
        });
  }

  private void perform(JBTable table, int row, Project project, Map<Todo, URI> urisByTodo) {
    if (row < 0 || row >= table.getRowCount()) {
      return;
    }
    var tableModel = (TodoTableModel) table.getModel();
    var todo = tableModel.getItemAt(row);
    ApplicationManager.getApplication()
        .invokeLater(
            () ->
                WriteAction.run(
                    () -> {
                      if (perform(todo, project, urisByTodo)) {
                        Files.delete(toFile(urisByTodo.get(todo)));
                        urisByTodo.remove(todo);
                        // TODO: This doesn't delete the row from the view
                        tableModel.fireTableRowsDeleted(row, row);
                      }
                    }));
  }

  private boolean perform(Todo todo, Project project, Map<Todo, URI> urisByTodo) {
    var todoUri = urisByTodo.get(todo);
    var baseDir = toBaseDir(todoUri);
    return ToolRunner.applySuggestion(
        todo.toTool(),
        todo.getCode(),
        todo.toLocation(),
        project,
        toInputSource(baseDir.getPath()),
        new FileOutputSink(baseDir));
  }

  private File toBaseDir(URI uri) {
    var result = toFile(uri);
    while (!"todo".equals(result.getName())) {
      result = result.getParentFile();
    }
    return result.getParentFile().getParentFile();
  }

  private File toFile(URI uri) {
    var path = uri.toString();
    if (path.startsWith(FILE_URI_SCHEME)) {
      path = path.substring(FILE_URI_SCHEME.length());
    }
    return new File(path);
  }

  private void performTodoOnAltEnter(
      @NotNull Project project, JBTable table, Map<Todo, URI> urisByTodo) {
    var inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    var actionMap = table.getActionMap();
    var altEnterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
    inputMap.put(altEnterKey, "do-task");
    actionMap.put(
        "do-task",
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            var row = table.getSelectedRow();
            perform(table, row, project, urisByTodo);
          }
        });
  }
}
