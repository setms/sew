package org.setms.sew.intellij.km;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KmStartupActivity implements ProjectActivity {

  @Override
  public @Nullable Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> ignored) {
    project.getService(KmSystemService.class).start();
    return null;
  }
}
