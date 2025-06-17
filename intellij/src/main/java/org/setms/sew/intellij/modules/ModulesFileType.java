package org.setms.sew.intellij.modules;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class ModulesFileType extends LanguageFileType {

  public static final ModulesFileType INSTANCE = new ModulesFileType();

  private ModulesFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Modules";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Modules";
  }

  @Override
  public @NotNull String getDescription() {
    return "Modules";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "modules";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.MODULES;
  }
}
