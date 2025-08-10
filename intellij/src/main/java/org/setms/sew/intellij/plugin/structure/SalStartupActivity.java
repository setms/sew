package org.setms.sew.intellij.plugin.structure;

import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sal.SalLanguage;

public class SalStartupActivity implements ProjectActivity {

  @Override
  public @Nullable Object execute(
      @NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    LanguageStructureViewBuilder.getInstance()
        .addExplicitExtension(SalLanguage.INSTANCE, new SalStructureViewFactory());
    return null;
  }
}
