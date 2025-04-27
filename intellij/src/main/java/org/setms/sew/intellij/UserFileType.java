package org.setms.sew.intellij;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserFileType extends LanguageFileType {

  public static final UserFileType INSTANCE = new UserFileType();

  private UserFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "User";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "User";
  }

  @Override
  public @NotNull String getDescription() {
    return "Stakeholder who uses the system";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "user";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.USER;
  }
}
