package org.setms.sew.intellij;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SewFileType extends LanguageFileType {
  public static final SewFileType INSTANCE = new SewFileType();

  private SewFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Sew File";
  }

  @Override
  public @NotNull String getDescription() {
    return "Sew language file";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "sew";
  }

  @Override
  public @Nullable Icon getIcon() {
    return IconLoader.getIcon("/icons/sew_icon.png", SewFileType.class);
  }
}
