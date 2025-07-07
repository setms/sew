package org.setms.sew.intellij.todo;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFileManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileListenerStartup implements ProjectActivity {

  @Override
  public @Nullable Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    project
        .getMessageBus()
        .connect(project)
        .subscribe(VirtualFileManager.VFS_CHANGES, new FileListener());
    return null;
  }
}
