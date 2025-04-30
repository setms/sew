package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class DecisionFileType extends LanguageFileType {

  public static final DecisionFileType INSTANCE = new DecisionFileType();

  private DecisionFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Decision";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Decision";
  }

  @Override
  public @NotNull String getDescription() {
    return "Architecture or design decision";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "decision";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.DECISION;
  }
}
