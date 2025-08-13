package org.setms.sew.intellij.plugin.km;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.sew.intellij.plugin.workspace.IntellijWorkspace;

@Service(Service.Level.PROJECT)
@RequiredArgsConstructor
public final class KmSystemService {

  private final AtomicBoolean started = new AtomicBoolean();
  private final CompletableFuture<Void> ready = new CompletableFuture<>();

  private final Project project;
  @Getter
  private KmSystem kmSystem;

  public void start() {
    if (!started.compareAndSet(false, true)) {
      return;
    }
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> {
              kmSystem = new IntellijKmSystem(project);
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
