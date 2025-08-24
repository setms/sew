package org.setms.sew.intellij.plugin.km;

import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.plugin.workspace.IntellijWorkspace;

final class FileListener implements BulkFileListener {

  private static final String BUILD_PATH = "%s.km%s".formatted(File.separator, File.separator);

  private final Project project;
  private final IntellijWorkspace workspace;
  private final String rootPath;

  FileListener(Project project, IntellijWorkspace workspace) {
    this.project = project;
    this.workspace = workspace;
    this.rootPath = workspace.getRoot().toNioPath().toString();
  }

  @Override
  public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
    // Nothing to do
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    var service = DumbService.getInstance(project);
    for (var event : events) {
      var file = toFile(event);
      if (file == null) {
        continue;
      }
      var path = file.toNioPath().toString();
      if (!path.startsWith(rootPath) || path.contains(BUILD_PATH)) {
        continue;
      }
      if (event instanceof VFileDeleteEvent) {
        service.smartInvokeLater(() -> onFileDeleted(file));
      } else {
        service.smartInvokeLater(() -> onFileChanged(file));
      }
    }
  }

  private VirtualFile toFile(VFileEvent event) {
    return switch (event) {
      case VFilePropertyChangeEvent ignored -> null;
      case VFileCopyEvent copyEvent -> copyEvent.findCreatedFile();
      default -> event.getFile();
    };
  }

  private void onFileDeleted(@NotNull VirtualFile file) {
    Optional.ofNullable(workspace.find(file))
        .ifPresent(resource -> runWriteCommandAction(project, () -> workspace.deleted(resource)));
  }

  private void onFileChanged(@NotNull VirtualFile file) {
    runWriteCommandAction(project, () -> workspace.changed(file));
  }
}
