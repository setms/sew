package org.setms.sew.intellij;

import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SewStartupActivity implements ProjectActivity {

  @Override
  public @Nullable Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    LanguageStructureViewBuilder.getInstance()
        .addExplicitExtension(SewLanguage.INSTANCE, new SewStructureViewFactory());
    return null;
  }
}
