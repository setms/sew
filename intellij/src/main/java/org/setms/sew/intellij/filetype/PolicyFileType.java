package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.language.sew.SewLanguage;

public class PolicyFileType extends LanguageFileType {

  public static final PolicyFileType INSTANCE = new PolicyFileType();

  private PolicyFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Policy";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Policy";
  }

  @Override
  public @NotNull String getDescription() {
    return "Policy that handles events and issues commands";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "policy";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.POLICY;
  }
}
