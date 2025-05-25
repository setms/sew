package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class ContextMapFileType extends LanguageFileType {

  public static final ContextMapFileType INSTANCE = new ContextMapFileType();

  private ContextMapFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "ContextMap";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Context map";
  }

  @Override
  public @NotNull String getDescription() {
    return "Context map";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "contextMap";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.CONTEXT_MAP;
  }
}
