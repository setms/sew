package org.setms.sew.intellij.tool;

import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.INFO;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiDocumentManager;
import java.io.File;
import java.net.URI;
import java.util.Objects;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;
import org.setms.sew.intellij.editor.VirtualFileInputSource;

public class ToolRunner {

  private static final String CREATED_PREFIX = "Created ";

  public static boolean applySuggestion(
      Tool tool,
      String code,
      Location location,
      Project project,
      VirtualFileInputSource source,
      FileOutputSink sink) {
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    var diagnostics = tool.apply(code, source, location, sink);
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
        .map(LocalFileSystem.getInstance()::refreshAndFindFileByIoFile)
        .filter(Objects::nonNull)
        .forEach(file -> FileEditorManager.getInstance(project).openFile(file, true));
    return errors.isEmpty();
  }
}
