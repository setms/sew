package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class DomainsFileType extends LanguageFileType {

  public static final DomainsFileType INSTANCE = new DomainsFileType();

  private DomainsFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Domains";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Domains";
  }

  @Override
  public @NotNull String getDescription() {
    return "Domains";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "domains";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.CONTEXT_MAP;
  }
}
