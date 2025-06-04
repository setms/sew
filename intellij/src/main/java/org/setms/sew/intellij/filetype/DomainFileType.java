package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class DomainFileType extends LanguageFileType {

  public static final DomainFileType INSTANCE = new DomainFileType();

  private DomainFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Domain";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Domain";
  }

  @Override
  public @NotNull String getDescription() {
    return "Domain";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "domain";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.CONTEXT_MAP;
  }
}
