package org.setms.sew.intellij.acceptancetest;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.Icon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.lang.acceptance.AcceptanceLanguage;

public class AcceptanceTestFileType extends LanguageFileType {

  public static final AcceptanceTestFileType INSTANCE = new AcceptanceTestFileType();

  private AcceptanceTestFileType() {
    super(AcceptanceLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "AcceptanceTest";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Acceptance test";
  }

  @Override
  public @NotNull String getDescription() {
    return "Acceptance test";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "acceptance";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.ACCEPTANCE;
  }
}
