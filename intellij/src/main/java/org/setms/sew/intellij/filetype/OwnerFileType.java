package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class OwnerFileType extends LanguageFileType {

  public static final OwnerFileType INSTANCE = new OwnerFileType();

  private OwnerFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Owner";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Owner";
  }

  @Override
  public @NotNull String getDescription() {
    return "Stakeholder who finances the development of the system";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "owner";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.OWNER;
  }
}
