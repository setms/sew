package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class CommandFileType extends LanguageFileType {

  public static final CommandFileType INSTANCE = new CommandFileType();

  private CommandFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Command";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Command";
  }

  @Override
  public @NotNull String getDescription() {
    return "Command instructing an aggregate or external system to do something";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "command";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.COMMAND;
  }
}
