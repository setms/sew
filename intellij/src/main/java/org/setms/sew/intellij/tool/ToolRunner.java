package org.setms.sew.intellij.tool;

import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.INFO;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import java.io.File;
import java.net.URI;
import java.util.Objects;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Workspace;

public class ToolRunner {

  private static final String CREATED_PREFIX = "Created ";

  public static boolean applySuggestion(
      BaseTool tool, String code, Location location, Project project, Workspace workspace) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    var diagnostics = tool.apply(code, workspace, location);
    var errors = diagnostics.stream().filter(d -> d.level() == ERROR).toList();
    errors.forEach(
        diagnostic ->
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Sew")
                .createNotification(diagnostic.message(), NotificationType.ERROR)
                .notify(project));
    diagnostics.stream()
        .filter(d -> d.level() == INFO)
        .map(Diagnostic::message)
        .filter(m -> m.startsWith(CREATED_PREFIX))
        .map(m -> m.substring(CREATED_PREFIX.length()).trim())
        .map(URI::create)
        .map(File::new)
        .filter(File::isFile)
        .map(ToolRunner::toVirtualFile)
        .filter(Objects::nonNull)
        .forEach(file -> FileEditorManager.getInstance(project).openFile(file, true));
    return errors.isEmpty();
  }

  private static VirtualFile toVirtualFile(File file) {
    VfsUtil.markDirtyAndRefresh(true, false, false, file);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }
}
