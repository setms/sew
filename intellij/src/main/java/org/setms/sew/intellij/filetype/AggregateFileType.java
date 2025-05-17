package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class AggregateFileType extends LanguageFileType {

  public static final AggregateFileType INSTANCE = new AggregateFileType();

  private AggregateFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Aggregate";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Aggregate";
  }

  @Override
  public @NotNull String getDescription() {
    return "Aggregate";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "aggregate";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.AGGREGATE;
  }
}
