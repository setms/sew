package org.setms.sew.intellij.km;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.WriteIntentReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.km.domain.model.tool.Tools;

public class KmStartupActivity implements ProjectActivity {

  @Override
  public @Nullable Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> ignored) {
    Tools.reload();
    ApplicationManager.getApplication()
        .invokeLater(
            () -> WriteAction.run(() -> project.getService(KmSystemService.class).start()));
    return null;
  }
}
