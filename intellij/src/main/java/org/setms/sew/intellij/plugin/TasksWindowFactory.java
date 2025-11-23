package org.setms.sew.intellij.plugin;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import java.awt.BorderLayout;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.sew.intellij.plugin.km.KmSystemService;

public class TasksWindowFactory implements ToolWindowFactory, DumbAware {

  private final DefaultListModel<Diagnostic> model = new DefaultListModel<>();

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    addContentTo(toolWindow);
    var service = project.getService(KmSystemService.class);
    if (service.isNotReady()) {
      service.whenReady().thenRun(() -> loadTasks(service.getKmSystem()));
    } else {
      loadTasks(service.getKmSystem());
    }
  }

  private void addContentTo(ToolWindow toolWindow) {
    var list = new JBList<>(model);
    list.setCellRenderer(new TaskRenderer());
    var scrollPane = new JBScrollPane(list);
    var panel = new JPanel(new BorderLayout());
    panel.add(scrollPane, BorderLayout.CENTER);
    var content = ContentFactory.getInstance().createContent(panel, "", false);
    toolWindow.getContentManager().addContent(content);
  }

  private void loadTasks(KmSystem kmSystem) {
    kmSystem.diagnosticsWithSuggestions().forEach(model::addElement);
  }
}
