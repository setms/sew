package org.setms.sew.intellij.usecase;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class UseCaseFileType extends LanguageFileType {

  public static final UseCaseFileType INSTANCE = new UseCaseFileType();

  private UseCaseFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Use case";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Use case";
  }

  @Override
  public @NotNull String getDescription() {
    return "Describes scenarios for user requirement";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "useCase";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.USE_CASE;
  }
}
