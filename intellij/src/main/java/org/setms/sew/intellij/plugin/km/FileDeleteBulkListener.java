package org.setms.sew.intellij.plugin.km;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.intellij.plugin.workspace.IntellijWorkspace;

@RequiredArgsConstructor
public final class FileDeleteBulkListener implements BulkFileListener {

  private final Project project;
  private final IntellijWorkspace workspace;

  @Override
  public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
    // Nothing to do
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    for (var event : events) {
      if (event instanceof VFileDeleteEvent deleteEvent) {
        var file = deleteEvent.getFile();
        DumbService.getInstance(project).smartInvokeLater(() -> onFileDeleted(file));
      }
    }
  }

  private void onFileDeleted(@NotNull VirtualFile file) {
    Optional.ofNullable(workspace.find(file))
        .ifPresent(
            resource ->
                WriteCommandAction.runWriteCommandAction(
                    project, () -> workspace.deleted(resource)));
  }
}
