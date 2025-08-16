package org.setms.sew.intellij.plugin.km;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.sew.intellij.plugin.workspace.IntellijWorkspace;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor
public final class KmSystemService implements Disposable {

  private final AtomicBoolean started = new AtomicBoolean();
  private final CompletableFuture<Void> ready = new CompletableFuture<>();

  private final Project project;
  @Getter private KmSystem kmSystem;

  @Override
  public void dispose() {
    // Nothing to do
  }

  public void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> {
              kmSystem = new IntellijKmSystem(project);
              var connection = project.getMessageBus().connect(this);
              connection.subscribe(VirtualFileManager.VFS_CHANGES, new FileDeleteBulkListener(project, getWorkspace()));
            });
    ready.complete(null);
  }

  public boolean isNotReady() {
    return !ready.isDone();
  }

  public CompletableFuture<Void> whenReady() {
    return ready;
  }

  public IntellijWorkspace getWorkspace() {
    return (IntellijWorkspace) kmSystem.getWorkspace();
  }
}
