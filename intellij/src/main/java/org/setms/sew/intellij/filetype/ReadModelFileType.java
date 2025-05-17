package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class ReadModelFileType extends LanguageFileType {

  public static final ReadModelFileType INSTANCE = new ReadModelFileType();

  private ReadModelFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "ReadModel";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Read model";
  }

  @Override
  public @NotNull String getDescription() {
    return "Read model";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "readModel";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.READ_MODEL;
  }
}
